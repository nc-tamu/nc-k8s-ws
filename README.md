# Prerequisites
* Docker desktop: https://www.docker.com/products/docker-desktop/
* Dockerhub account: https://hub.docker.com/

# Docker session
## Build the application docker container
To start the build process, run the following command which targets the 'application' folder:

Example: `docker build -t docker-hub-username/name-of-container:version application\.`

This will build the docker container in the current directory and tag it with according to the parameter defined in `-t`. An example of a real world build is: 

`docker build -t nctamu/nc-k8s-ws:1.0.0 application\.`

This builds a container under the `nctamu` user under the `nc-k8s-ws` repository on docker hub and tagged with version 1.0.0

## Push the docker container
When the container image is built, it needs to be pushed to docker hub. This can be done by running:

`docker push docker-hub-username/name-of-container:version`

Example: `docker push nctamu/nc-k8s-ws:1.0.0`

This will push the image to docker hub and look like this:

```
docker push nctamu/nc-k8s-ws:1.0.0
The push refers to repository [docker.io/nctamu/nc-k8s-ws]
0714e7311bcd: Pushed
dc9fa3d8b576: Mounted from library/openjdk
27ee19dc88f2: Mounted from library/openjdk
c8dd97366670: Mounted from library/openjdk
```

If the system complains about missing login, you need to run:

`docker login`

and enter the username and password given for the user in docker hub and them attempt to push again. 

Ps. if using a private registry, it will be necessary to provide the full url to the `docker login` command.

## Start the docker container locally
The container can be started immediately after the image is built locally so it is not required to be pushed to an external registry. To start the application locally, use the following command:

`docker run -it -p 8080:8080 docker-hub-username/name-of-container:version`

Example: `docker run -it -p 8080:8080 nctamu/nc-k8s-ws:1.0.0` 

Then the application can be accessed on: `http://app.127.0.0.1.nip.io:8080/`

# Kubernetes session

## Starting local kubernetes cluster
To start a local kubernetes cluster based on the k3d distribution, run the following command in the project root directory from powershell

`.\bin\k3d.exe cluster create --config .\config.yml`

When the cluster is started, check that you are able to communicate with the cluster by retrieving a list of all active nodes in the cluster:

`.\bin\kubectl.exe get nodes`

which should yield a result like this:
```
NAME                     STATUS   ROLES                  AGE    VERSION
k3d-mycluster-server-0   Ready    control-plane,master   116s   v1.27.4+k3s1
k3d-mycluster-agent-0    Ready    <none>                 113s   v1.27.4+k3s1
k3d-mycluster-agent-1    Ready    <none>                 112s   v1.27.4+k3s1
```

### Import image locally (if not using dockerhub)
`./bin/k3d.exe image import nctamu/nc-k8s-ws:1.0.0 --cluster mycluster`

## Deploy application to kubernetes
Interaction with kubernetes is normally done using the cli tool `kubectl` and the same applies for deployment. Open powershell again in the root directory and run `.\bin\kubectl.exe apply -k .\application\k8s-simple\`

This will read the "kustomize.yaml" file and deploy it to the current cluster. 

Ps. "Kustomize" is just another tool to help us merge multiple kubernetes configuration files and deploy in a single command. The clean kubernetes method is to run `kubectl apply -f deployment.yaml` and then do it for the other yaml files individually. 

The result looks like this:
```
PS C:\...\k8s> .\bin\kubectl.exe apply -k .\application\k8s-simple\
service/application-svc created
deployment.apps/application-deployment created
ingress.networking.k8s.io/application-ingress created
```

To check if everything has started, the following commands can be executed.

Get pods:
```
PS C:\...\k8s> .\bin\kubectl.exe get pods
NAME                                     READY   STATUS    RESTARTS   AGE
application-deployment-cd6bb685b-c4pl6   1/1     Running   0          2m7s
```


Get services:
```
PS C:\...\k8s> .\bin\kubectl.exe get svc
NAME              TYPE        CLUSTER-IP     EXTERNAL-IP   PORT(S)    AGE
application-svc   ClusterIP   10.43.223.85   <none>        8080/TCP   2m17s
```

Get Ingress:
```
PS C:\...\k8s> .\bin\kubectl.exe get ingress
NAME                  CLASS     HOSTS                  ADDRESS                            PORTS   AGE
application-ingress   traefik   app.127.0.0.1.nip.io   172.19.0.3,172.19.0.4,172.19.0.5   80      2m29s
```

In this scenario it will now be possible to open the url `http://app.127.0.0.1.nip.io:8080/` in your browser and see the hostname of the container serving the request just like in the first example. 

Ps. the reason for adding port `:8080` is because we configured the port `8080` to be mapped to port `80` inside the cluster, see `config.yml` so this is just applicable for this local environment.

Notice how there is now more information displayed. Now you can see the pods inside the namespace as well as the health status of them.

### Change probe for application via browser
To toggle the liveness value of the pod, click the `Toggle Liveness on Random pod` button and then the `Refresh Page` button. You should see that the pod has changed from GOOD to BAD. This indicates that the application is not healthy. Wait 30 seconds and refresh the page again. Now you should see that it is GOOD again.

Tip, if you can, have your terminal and browser side by side.

This is because Kubernetes automatically detected the BAD state and restarted the application. If you get pods again you can see that the restart counter has gone up!

```
PS C:\...\k8s> .\bin\kubectl.exe get pods
NAME                                     READY   STATUS    RESTARTS     AGE
application-deployment-cd6bb685b-c4pl6   1/1     Running   1 (14s ago)  2m7s
```

You can also `describe` the pod to see details of what happened:
```
PS C:\...\k8s> .\bin\kubectl.exe descibe pods application-deployment-cd6bb685b-c4pl6
Events:
  Type     Reason     Age                  From               Message
  ----     ------     ----                 ----               -------
  Normal   Scheduled  8m51s                default-scheduler  Successfully assigned
  Warning  Unhealthy  41s (x3 over 61s)    kubelet            Liveness probe failed: HTTP probe failed with statuscode: 500
  Normal   Killing    41s                  kubelet            Container application-pod failed liveness probe, will be restarted
  Normal   Pulled     40s (x2 over 8m51s)  kubelet            Container image "nctamu/nc-k8s-ws:1.0.0" already present on machine
  Normal   Created    40s (x2 over 8m51s)  kubelet            Created container application-pod
  Normal   Started    40s (x2 over 8m51s)  kubelet            Started container application-pod
```

Notice how it reports that the application had `Liveness probe failed`. This is because we defined the endpoint of the application to checks its health on /liveness, but since we forced it to report code 500, Kubernetes thinks it is unhealthy.

### Change probe for application via cli (if you want to try)
To check and toggle the value of `liveness` using `curl`, follow these steps:

Step 1: Start the Spring Boot Application
- Make sure you have the Spring Boot application running.

Step 2: Check the Current Liveness Status
- Open a terminal or command prompt.

Step 3: Send a GET Request to Check Liveness
- Use the following `curl` command to send a GET request and check the current liveness status (replace with app.127.0.0.1.nip.io:8080 if docker/k8s):

```bash
curl -I -X GET http://localhost:PORT/liveness
```
```ps
Invoke-RestMethod -Uri 'http://localhost:PORT/liveness' -Method Get -Verbose
```

Replace `PORT` with the port number on which your Spring Boot application is running.

This will either return a `200 OK` if `liveness` is `true` or a `500 Internal Server Error` if `liveness` is `false`.

Step 4: Toggle the Liveness Status
- To toggle the liveness status, send a POST request:

```bash
curl -v -X POST http://localhost:PORT/liveness
```
```ps
Invoke-RestMethod -Uri 'http://localhost:PORT/liveness' -Method Post -Verbose
```


Again, replace `PORT` with the actual port number.

Step 5: Check the Updated Liveness Status
- After sending the POST request, use the GET request again to check the updated liveness status:

```bash
curl -I -X GET http://localhost:PORT/liveness
```
```ps
Invoke-RestMethod -Uri 'http://localhost:PORT/liveness' -Method Get -Verbose
```

This time, it should return the opposite response compared to what you received in Step 3.

You can repeat Steps 4 and 5 to toggle and check the liveness status as many times as you want. Keep in mind that `liveness` will toggle its value with each POST request.

## Scale the application
Having application redundancy is important for stability. Now we will try to scale the application to more instances:
```
PS C:\...\k8s> .\bin\kubectl.exe scale deployment application-deployment --replicas=2
deployment.apps/application-deployment scaled
```

```
PS C:\...\k8s> .\bin\kubectl.exe get pods
NAME                                      READY   STATUS    RESTARTS        AGE
application-deployment-6994ccd745-rmpr8   1/1     Running   1 (4m31s ago)   12m
application-deployment-6994ccd745-9psb9   1/1     Running   0               26s
```

As seen above the application now has two replicas. If you visit http://app.127.0.0.1.nip.io:8080/ again you will notice that it now also lists the second application.

You can now try to change the liveness once and refresh the page. Now only one instance will be BAD, however, the application is still available since it has two running replicas!

Using multiple replicas is a good way to scale your application horizontally but beware not to scale too much as it will use a lot of resources if not done properly.

## Pretty overview of kubernetes (K9S)
To get an easy overview of what is running in your cluster, the tool `k9s` is one of many good tools to interact with the cluster and see what is running. 

An alternative is named `openlens` if text based UI is not to your liking.

# Kubernetes Cleanup
To delete the local cluster again, run the following command:

`.\bin\k3d.exe cluster delete mycluster`