# Deploying the Round Trip backend

Same flow as larouedugras: GitHub Actions builds the image into GHCR, records the tag in
`deploy/kustomization.yaml`, and ArgoCD syncs `deploy/` into the `round-trip` namespace.

- One replica, ClusterIP Service, non-root, read-only root filesystem.
- `/readyz` is the readiness probe, so the pod has no Service endpoints until
  `PRIM_API_KEY` exists. STAR-only use needs a placeholder value. The variable is `optional`,
  so the pod still starts and stays live before the secret exists.
- Nothing is public. The Tailscale Ingress (`tailscale-ingress.yaml`) is not in the
  kustomization because the Tailscale operator is not installed on the cluster yet.

## One-time setup

1. **Secrets, by hand, never committed** (the repo is public):

       kubectl create namespace round-trip
       kubectl -n round-trip create secret generic round-trip-secrets \
         --from-literal=PRIM_API_KEY='<your key>'

   Only if the image package stays private, also a pull secret (PAT with `read:packages`):

       kubectl -n round-trip create secret docker-registry ghcr-pull \
         --docker-server=ghcr.io --docker-username=gauthiercpx --docker-password='<token>'

   Making the package public after its first push avoids this secret.
2. **Register the app:** copy `deploy/argocd/round-trip-application.yaml` to `apps/round-trip.yaml`
   in the homelab repo and push. ArgoCD needs read access to this repository over SSH.
3. **First image:** merge to `main`. The `backend` workflow tests, pushes
   `ghcr.io/gauthiercpx/round-trip-backend:sha-xxxxxxx` and commits that tag here.
   No GitHub secret is needed: it uses the run's `GITHUB_TOKEN`. If `main` requires pull
   requests, allow the bot to bypass it so the tag commit can land.

## Verify

    kubectl -n round-trip rollout status deploy/round-trip-backend
    kubectl -n round-trip port-forward svc/round-trip-backend 8080:80
    curl localhost:8080/healthz
    curl 'localhost:8080/departures?stops=star-metro:5074'

Once the Tailscale operator exists, add `tailscale-ingress.yaml` back to the kustomization and
use `https://round-trip.<tailnet>.ts.net`; that HTTPS address is what the Android app needs.

## Local image check

    docker build -t round-trip-backend:local backend/
    docker run --rm -p 8080:8080 --read-only --tmpfs /tmp round-trip-backend:local
