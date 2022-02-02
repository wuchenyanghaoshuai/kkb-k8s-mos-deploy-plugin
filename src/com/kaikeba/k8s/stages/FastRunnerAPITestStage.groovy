

package com.kaikeba.k8s.stages

import groovyx.net.http.RESTClient
import static groovyx.net.http.Method.*
import static groovyx.net.http.ContentType.*
import org.yubing.delivery.Stage
import com.kaikeba.k8s.stages.UndoDeployStage

/**
 * 通过FastRunner执行接口测试
 */
@GrabResolver(name='aliyun', root='http://maven.aliyun.com/nexus/content/groups/public/')
@Grapes([
  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1')
])
class FastRunnerAPITestStage extends Stage {

    def script;
    def config;
    def deployEnv;
    def testType;
    def undoDeployState;
    def fastRunnerConfig;

    FastRunnerAPITestStage(project, stageName, deployEnv, testType) {
        super(project, stageName)

        this.script = project.script;
        this.config = project.config;
        this.deployEnv = deployEnv;
        this.testType = testType;
        this.undoDeployState = new UndoDeployStage(project, stageName, deployEnv);

        def debug = false
        if (debug) {
          this.fastRunnerConfig = [
            apiUrl: "http://yapi.kaikeba.com/mock/1278/", // API服务器地址
            token : '62bdbeeedcdd5ac11c3e20a69818b0ff' // Token
          ]
        } else {
          this.fastRunnerConfig = [
            apiUrl: "http://192.168.100.75:8113/", // API服务器地址
            token : '62bdbeeedcdd5ac11c3e20a69818b0ff' // Token
          ]
        }
    }
    
    def run() {
        this.script.echo "config: ${this.config}"

        def group = this.config.group
        def name = this.config.name
        def deployEnv = this.deployEnv
        def testType = this.testType

        this.script.node('jenkins-slave-k8s') {
          try {
            this.apiTest(group, name, deployEnv, testType);
          } catch(e) {
//            this.undoDeploy(e);
          }
        }
    }

    /**
      * API 接口测试
      */
    def apiTest(group, name, deployEnv, testType) {
        this.script.echo "开始接口测试..."

        def task = this.runTestSuit(group, name, deployEnv, testType)
        if (task) {
          def taskId = task.task_id

          def taskInfo = this.queryTaskInfo(taskId)
          while(taskInfo.status == "PENDING") {
            this.script.echo "休眠5s..."
            sleep(1000) // 休眠1s

            taskInfo = this.queryTaskInfo(taskId)
          }

          def buildNo = this.script.env.BUILD_NUMBER

          if (taskInfo.status == "SUCCESS") {
            this.script.echo "接口测试通过！"
            this.script.env.DEPLOY_RESULT="${buildNo}.ok"

            if (taskInfo.report_url) {
              this.script.echo "测试报告地址：${taskInfo.report_url}"
            }
          } else {
            this.script.echo "接口测试失败！"
            this.script.env.DEPLOY_RESULT="${buildNo}.undo"
            
            if (taskInfo.report_url) {
              this.script.echo "测试报告地址：${taskInfo.report_url}"
            }

            throw new RuntimeException("接口测试失败，${taskInfo.msg}")
          }
        } else {
          this.script.echo "没有配置测试用例，跳过"
        }
    }

    /**
      * 运行测试用例
      */
    def runTestSuit(group, name, deployEnv, testType) {
        this.script.echo "组:${group}, 服务:${name}, 环境:${deployEnv}, 测试类型:${testType}, 运行测试用例...";

        def client = new RESTClient(this.fastRunnerConfig.apiUrl)
        
        def uriPath = "api/fastrunner/run_test_suit/"

        def params = [
          token: this.fastRunnerConfig.token
        ]

        

        def postBody = [
          server_name : name,
          case_type: testType,
          env: deployEnv
        ]

        if (group!=null && !"".equals(group)) {
          postBody.put("project_name", group)
        } else {
          postBody.put("project_name", "null")
        }

        this.script.echo "URL: ${this.fastRunnerConfig.apiUrl}${uriPath}"
        this.script.echo "params: ${params}"
        this.script.echo "PostBody: ${postBody}"

        def resp = client.post(path:uriPath, params:params, body:postBody, requestContentType: URLENC)
        
        this.script.echo "runTestSuit status: ${resp.status}"
        this.script.echo "runTestSuit Result: ${resp.data}"

        assert resp.status == 200

        if (resp.data && resp.data.task_info) {
          return resp.data.task_info
        } else if (resp.data && resp.data.code==2) {
          return null
        } else {
          throw new RuntimeException("运行测试用例失败: ${resp.data.msg}")
        }
    }

    /**
      * API 接口测试
      */
    def queryTaskInfo(taskId) {
        this.script.echo "任务ID:${taskId}， 查询任务状态...";

        def client = new RESTClient(this.fastRunnerConfig.apiUrl)
        
        def uriPath = "api/fastrunner/run_test_suit/info/"
        def params = [
          task_id: taskId,
          server_name: this.config.name,
          token: this.fastRunnerConfig.token
        ]
        
        this.script.echo "URL: ${this.fastRunnerConfig.apiUrl}${uriPath}"
        this.script.echo "Params: ${params}"

        def resp = client.get(path:uriPath, params:params)

        this.script.echo "taskInfo status: ${resp.status}"
        assert resp.status == 200

        this.script.echo "taskInfo Result: ${resp.data}"

        return resp.data
    }

    /**
      * API 接口测试
      */
    def getTest() {
        this.script.echo "测试...";

        def client = new RESTClient("https://www.baidu.com")
        
        def uriPath = "/"
        this.script.echo "URL: ${this.fastRunnerConfig.apiUrl}${uriPath}"

        def resp = client.get(path:uriPath)

        this.script.echo "测试 status: ${resp.status}"
        assert resp.status == 200

        this.script.echo "测试 Result: ${resp.data}"
    }

    /**
      * 回滚
      */
    def undoDeploy(e) {
        this.script.echo "冒烟测试失败:" + e.message;
        this.script.echo "开始回滚...";

        this.undoDeployState.run();

        throw new RuntimeException("回滚完成，for: ${e.message}")
    }
}