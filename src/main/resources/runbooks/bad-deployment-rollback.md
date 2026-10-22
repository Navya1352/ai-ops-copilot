# Runbook: Bad Deployment

## Symptoms
- Error rate rises sharply (for example above 10%) right after a new version is deployed
- Logs contain new exceptions such as NullPointerException
- Logs show "Started version" with the new version number
- CPU, memory, latency and database connections look mostly normal

## Diagnosis
1. Check the service version: did it change recently?
2. Check the logs: did the errors start right after "Started version vX.Y.Z"?
3. Compare with the previous version: if it was healthy, the new release is the likely cause.

## Remediation
1. Roll back to the previous known-good version. Rollback is the correct fix.
2. Do NOT rely on a restart: restarting reloads the same faulty code and the errors continue.
3. Notify the team that shipped the release and block further deploys until it is fixed.

## Escalation
If errors continue after rollback, page the platform on-call engineer.