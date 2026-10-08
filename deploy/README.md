# Deploying the Round Trip backend

Same flow as larouedugras: GitHub Actions builds the image into GHCR, records the tag in
`deploy/kustomization.yaml`, and ArgoCD syncs `deploy/` into the `round-trip` namespace.

- One replica, ClusterIP Service, non-root, read-only root filesystem.
- `/readyz` is the readiness probe, so the pod has no Service endpoints until
  `PRIM_API_KEY` exists. STAR-only use needs a placeholder value. The variable is `optional`,
  so the pod still starts and stays live before the secret exists.
- Nothing is public. The backend is exposed only on the tailnet through the Tailscale
  operator (`tailscale-ingress.yaml`), with HTTPS on `round-trip.<tailnet>.ts.net`.

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

## Public access for the phone (Cloudflare tunnel + bearer token)

The phone does not need Tailscale: a dedicated tunnel publishes only `GET /departures` at
`round-trip.gauthiercpx.dev` (the zone must be in the same Cloudflare account as the tunnel; change the hostname in
`cloudflared-config.yaml` to use another). `/healthz` and `/readyz` are not routed. The backend refuses every
`/departures` request without `Authorization: Bearer <API_TOKEN>`, and refuses all of them when no
token is configured.

1. Create the tunnel and its DNS record, from a machine that has `cloudflared`:

       cloudflared tunnel login
       cloudflared tunnel create round-trip
       cloudflared tunnel route dns round-trip round-trip.gauthiercpx.dev

2. Give the cluster the tunnel key (it is not committed):

       kubectl -n round-trip create secret generic cloudflared-credentials \
         --from-file=credentials.json=$HOME/.cloudflared/<tunnel-id>.json

3. Create the API token in the existing secret. This generates it and stores it without printing it:

       kubectl -n round-trip patch secret round-trip-secrets --type merge \
         -p "{\"stringData\":{\"API_TOKEN\":\"$(openssl rand -base64 32)\"}}"

   Read it once to type into the app's settings screen:

       kubectl -n round-trip get secret round-trip-secrets -o jsonpath='{.data.API_TOKEN}' | base64 -d

4. Restart the backend so it picks the token up:

       kubectl -n round-trip rollout restart deploy/round-trip-backend

To rotate the token, repeat steps 3 and 4 and update the app.

## Verify

    kubectl -n round-trip rollout status deploy/round-trip-backend
    kubectl -n round-trip port-forward svc/round-trip-backend 8080:80
    curl localhost:8080/healthz
    curl -H "Authorization: Bearer $TOKEN" 'localhost:8080/departures?stops=star-metro:5074'
    curl -i 'https://round-trip.gauthiercpx.dev/departures?stops=star-metro:5074'   # expect 401

Over the tailnet use `https://round-trip.<tailnet>.ts.net`; that HTTPS address is what the
Android app needs. The ingress status shows the exact name:

    kubectl -n round-trip get ingress

## Local image check

    docker build -t round-trip-backend:local backend/
    docker run --rm -p 8080:8080 --read-only --tmpfs /tmp round-trip-backend:local
