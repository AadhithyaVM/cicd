# Screenshot checklist (Deliverable 3 & 4) – save in docs/screenshots/
1. Azure Portal -> Resource group cia2-rg (VMs, registry, network) and `az vm list -d -o table`
2. `kubectl get nodes` (cluster Ready) on the Kubernetes VM
3. Azure Portal -> Container registry -> Repositories -> devops-demo (tags 1, 2, latest)
4. `kubectl get deploy,pods,svc -n cia2-demo` (Running, 2/2 ready, NodePort 30080)
5. Browser at http://K_PUB:30080 showing version 1
6. Jenkins Stage View – all stages green (+ test result trend)
7. GitHub commit for the change
8. Jenkins build #2 triggered by the push
9. Registry with the new tag; browser showing version 2; `kubectl rollout history`
