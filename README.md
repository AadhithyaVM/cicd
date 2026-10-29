# CIA 2 – End-to-End CI/CD Pipeline on Microsoft Azure

Java web app → **Git/GitHub** → **Jenkins** → **Maven** (build + JUnit) → **Docker** → **Azure Container Registry** → **Ansible** → **Kubernetes (k3s on an Azure VM)** → browser.

```
src/                      Java app + JUnit tests (9 tests)
pom.xml                   Maven build (Java 17, JUnit 4, executable jar target/app.jar)
Dockerfile, .dockerignore Container image (non-root, healthcheck)
Jenkinsfile               checkout > package > test > docker build > push to ACR > ansible deploy > verify
k8s/                      namespace, deployment (2 replicas, probes, rolling update), service (NodePort 30080)
ansible/                  setup-k8s.yml, setup-jenkins.yml, deploy.yml (+ Jenkins init script template)
scripts/                  bootstrap-on-jenkins.sh (runs on the Jenkins VM)
docs/                     architecture diagram, screenshot checklist
```

All commands below are for **Windows PowerShell**. Follow the steps in order; every step depends on the one before it.

---

## Step 0 – Tools and login (once)
```powershell
winget install Microsoft.AzureCLI          # then close and reopen PowerShell
az login                                   # sign in with your Azure for Students account
az account show --query "{name:name, id:id}" -o table
```
You already have Git and ssh. If `az account show` lists more than one subscription, run `az account set --subscription "<name>"`.

## Step 1 – Put the project on GitHub (once)
```powershell
cd E:\CICD
git init
git add .
git commit -m "CI/CD project"
git branch -M main
git remote add origin https://github.com/AadhithyaVM/cicd-cia2.git
git push -u origin main
```
(Skip `git init` / `git remote add` if you already did them. If you only replaced files in an existing repo: `git add .`, `git commit -m "Azure version"`, `git push`.)
The repo must be **public** so that the Jenkins VM can clone it.

## Step 2 – Variables (re-run this block whenever you open a new PowerShell window)
```powershell
$RG  = "cia2-rg"
$LOC = "centralindia"          # if Azure says the region is not allowed, try: southindia, southeastasia, uksouth
$KEY = "$HOME\.ssh\cia2-key"
```

## Step 3 – Register Azure services (once, a few minutes)
```powershell
az provider register --namespace Microsoft.Compute --wait
az provider register --namespace Microsoft.Network --wait
az provider register --namespace Microsoft.ContainerRegistry --wait
```

## Step 4 – Resource group and container registry
```powershell
az group create --name $RG --location $LOC

$ACR = "cia2acr" + (Get-Random -Maximum 99999)      # must be globally unique, lowercase
az acr create --resource-group $RG --name $ACR --sku Basic --admin-enabled true --location $LOC

$ACR_SERVER = az acr show --name $ACR --query loginServer -o tsv
$ACR_USER   = az acr credential show --name $ACR --query username -o tsv
$ACR_PASS   = az acr credential show --name $ACR --query "passwords[0].value" -o tsv
$ACR_SERVER                                          # prints something like cia2acr12345.azurecr.io
```

## Step 5 – Network and firewall
```powershell
az network vnet create --resource-group $RG --name cia2-vnet --address-prefix 10.10.0.0/16 --subnet-name default --subnet-prefix 10.10.1.0/24
az network nsg create --resource-group $RG --name cia2-nsg

$MYIP = (Invoke-RestMethod https://api.ipify.org).Trim()
az network nsg rule create --resource-group $RG --nsg-name cia2-nsg --name ssh     --priority 1000 --access Allow --protocol Tcp --direction Inbound --destination-port-ranges 22    --source-address-prefixes $MYIP
az network nsg rule create --resource-group $RG --nsg-name cia2-nsg --name jenkins --priority 1010 --access Allow --protocol Tcp --direction Inbound --destination-port-ranges 8080
az network nsg rule create --resource-group $RG --nsg-name cia2-nsg --name app     --priority 1020 --access Allow --protocol Tcp --direction Inbound --destination-port-ranges 30080
```
Traffic between the two VMs inside the virtual network is allowed by Azure's default rules (this is how Jenkins reaches Kubernetes).

## Step 6 – SSH key and the two VMs (about 5 minutes)
```powershell
ssh-keygen -t rsa -b 3072 -f $KEY           # press Enter twice (empty passphrase)

az vm create --resource-group $RG --name cia2-jenkins --image Ubuntu2204 --size Standard_B2s --admin-username ubuntu --authentication-type ssh --ssh-key-values "$KEY.pub" --vnet-name cia2-vnet --subnet default --nsg cia2-nsg --nsg-rule NONE --public-ip-sku Standard --public-ip-address-allocation static

az vm create --resource-group $RG --name cia2-k8s --image Ubuntu2204 --size Standard_B2s --admin-username ubuntu --authentication-type ssh --ssh-key-values "$KEY.pub" --vnet-name cia2-vnet --subnet default --nsg cia2-nsg --nsg-rule NONE --public-ip-sku Standard --public-ip-address-allocation static
```
If `Standard_B2s` is not available for your subscription, repeat both commands with `Standard_B2as_v2`, `Standard_D2s_v3` or `Standard_B2ms` (use the same size for both). Public IPs are static, so they do not change when you stop and start the VMs.

Get the addresses:
```powershell
$J_PUB  = az vm show -d --resource-group $RG --name cia2-jenkins --query publicIps  -o tsv
$K_PUB  = az vm show -d --resource-group $RG --name cia2-k8s     --query publicIps  -o tsv
$K_PRIV = az vm show -d --resource-group $RG --name cia2-k8s     --query privateIps -o tsv
$J_PUB; $K_PUB; $K_PRIV
```

## Step 7 – Copy files to the Jenkins VM
```powershell
icacls $KEY /inheritance:r
icacls $KEY /grant:r "$($env:USERNAME):(R)"

@{ repo_url="https://github.com/AadhithyaVM/cicd-cia2.git"; repo_branch="main"; jenkins_admin_password="Cia2@Admin123"; acr_server=$ACR_SERVER; acr_user=$ACR_USER; acr_password=$ACR_PASS } | ConvertTo-Json | Set-Content -Encoding ascii cia2-vars.json

scp -i $KEY -o StrictHostKeyChecking=no $KEY ubuntu@${J_PUB}:/home/ubuntu/.ssh/cia2-key.pem
scp -i $KEY -o StrictHostKeyChecking=no cia2-vars.json ubuntu@${J_PUB}:/home/ubuntu/cia2-vars.json
scp -i $KEY -o StrictHostKeyChecking=no scripts\bootstrap-on-jenkins.sh ubuntu@${J_PUB}:/home/ubuntu/bootstrap.sh
Remove-Item cia2-vars.json                          # it contains the registry password, do not commit it
```

## Step 8 – Let Ansible configure everything (10–15 minutes)
```powershell
ssh -i $KEY -o StrictHostKeyChecking=no ubuntu@$J_PUB
```
Your prompt changes to `ubuntu@cia2-jenkins`. Now run, replacing `10.10.1.5` with the value of `$K_PRIV` you printed in step 6:
```bash
sed -i 's/\r$//' bootstrap.sh
bash bootstrap.sh 10.10.1.5 https://github.com/AadhithyaVM/cicd-cia2.git main
exit
```
Ansible installs k3s (Kubernetes) on the second VM, then installs Jenkins on this one and creates the admin user, the credentials and the pipeline job. The **first build starts by itself**.

## Step 9 – See it running
```powershell
"Jenkins:  http://${J_PUB}:8080     (user: admin, password: Cia2@Admin123)"
"App:      http://${K_PUB}:30080"
```
Open Jenkins → `cia2-pipeline` → build #1 should turn green (about 3–5 minutes). Then open the app URL: *"Hello from the CI/CD pipeline - version 1"*.

## Step 10 – Change and redeploy (the demo)
Edit `HEADLINE` in `src/main/java/com/cia2/app/GreetingService.java`, then:
```powershell
git add .
git commit -m "Update headline to version 2"
git push
```
Jenkins checks GitHub every 2 minutes (or click **Build Now**). When the build is green, refresh `http://<K_PUB>:30080`: new text and build number 2.

## Step 11 – Evidence for screenshots
```powershell
az resource list --resource-group $RG -o table                                   # cloud resources
az vm list --resource-group $RG -d -o table                                      # VMs
az acr repository show-tags --name $ACR --repository devops-demo -o table        # images in the registry
ssh -i $KEY ubuntu@$K_PUB "sudo k3s kubectl get nodes"
ssh -i $KEY ubuntu@$K_PUB "sudo k3s kubectl get deploy,pods,svc -n cia2-demo -o wide"
ssh -i $KEY ubuntu@$K_PUB "sudo k3s kubectl rollout history deployment/devops-demo -n cia2-demo"
```
Also take screenshots of the Azure Portal (resource group, registry repository), Jenkins Stage View and the browser (see `docs/SCREENSHOT_CHECKLIST.md`).

## Pause, resume, delete
```powershell
az vm deallocate --resource-group $RG --name cia2-jenkins      # pause (stops VM charges; data is kept)
az vm deallocate --resource-group $RG --name cia2-k8s

az vm start --resource-group $RG --name cia2-jenkins           # resume (same IP addresses)
az vm start --resource-group $RG --name cia2-k8s

az group delete --name $RG --yes                               # delete EVERYTHING after the review
```
After a resume the app comes back by itself in 1–2 minutes. If you opened a new PowerShell window, run the step 2 block and then:
```powershell
$ACR    = az acr list --resource-group $RG --query "[0].name" -o tsv
$J_PUB  = az vm show -d --resource-group $RG --name cia2-jenkins --query publicIps -o tsv
$K_PUB  = az vm show -d --resource-group $RG --name cia2-k8s     --query publicIps -o tsv
```
If your home IP changes (new Wi-Fi), SSH is blocked by the firewall rule; update it:
```powershell
az network nsg rule update --resource-group $RG --nsg-name cia2-nsg --name ssh --source-address-prefixes (Invoke-RestMethod https://api.ipify.org).Trim()
```

## Run locally (optional)
```powershell
mvn clean package            # builds target/app.jar and runs the 9 unit tests
java -jar target/app.jar     # http://localhost:8080
```

## Troubleshooting
| Symptom | Fix |
|---|---|
| `RequestDisallowedByPolicy` / region not allowed | Use another `$LOC` (southindia, southeastasia, uksouth) and re-run from step 4 |
| VM size not available / quota exceeded | Use another size from step 6 for both VMs |
| `Permissions ... too open` on the key | Re-run the two `icacls` lines in step 7 |
| Jenkins build fails at *Push to Azure Container Registry* | Check `acr_user` / `acr_password` in the vars file; `az acr credential show --name $ACR` |
| Pods `ImagePullBackOff` | The deploy stage recreates the `acr-secret`; re-run the build |
| Build does not start after `git push` | Click **Build Now**; polling runs every 2 minutes |
