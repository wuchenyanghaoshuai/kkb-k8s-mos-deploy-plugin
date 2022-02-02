# kkb k8s rollout plugin
# 此pipeline为后端java项目
```
1. 需要修改的地方为: AccessControlStage.groovy
第53行: 添加一个为admin的jenkins的账号,此处我添加了一个devops
```
![image](https://user-images.githubusercontent.com/39818267/152105397-0915c997-d458-47e7-bc25-9bd639a41feb.png)
```
2.修改BuildImageStage.groovy
修改133行附近: dev || test 环境修改为自己的harbor地址即可
resigtryLogin 这个位置是修改为jenkisn的token,创建一个凭据，账号能连上harbor即可，然后把token复制过来
prod环境我这边使用的是阿里云的景象仓库，
```
![image](https://user-images.githubusercontent.com/39818267/152105494-6b965dd9-2b9b-4fc1-8f67-d67217f83e92.png)

```
3.修改PullScriptStage.groovy
修改26-29行: 需要修改一下git地址，然后在该gitlab上创建一个kkb-release组
credentialsId 这块的token是 在jenkins创建一个账号去gitlab里去拉代码的
```
![image](https://user-images.githubusercontent.com/39818267/152105614-e4751ef2-7829-4144-9952-77ba967fe79b.png)
