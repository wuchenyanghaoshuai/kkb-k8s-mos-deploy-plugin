package com.kaikeba.k8s

import org.yubing.delivery.Project
import org.yubing.delivery.plugin.Plugin
import com.kaikeba.k8s.stages.BuildImageStage
import com.kaikeba.k8s.stages.DevDeployStage
import com.kaikeba.k8s.stages.KmosDevDeployStage
import com.kaikeba.k8s.stages.QADeployStage
import com.kaikeba.k8s.stages.QA2DeployStage
import com.kaikeba.k8s.stages.PreDeployStage
import com.kaikeba.k8s.stages.ProdDeployStage
import com.kaikeba.k8s.stages.AccessControlStage
import com.kaikeba.k8s.stages.ConfirmMessgerStage
import com.kaikeba.k8s.stages.AutoMergeStage
import com.kaikeba.k8s.stages.ConfirmDeployOKStage
import com.kaikeba.k8s.stages.UndoDeployStage
import com.kaikeba.k8s.stages.FastRunnerAPITestStage
import com.kaikeba.k8s.stages.MasterCheckStage

class MosDeployPlugin implements Plugin<Project> {

    def wholeFlow(project) {
        project.pipeline("WHOLE_FLOW", [
//            "AccessControlDev",
            // Build
            "UploadTestImage",
            "UploadProdImage",
//            "部署Dev环境",
            "DeployKmosDev",

            // DeployQA
            "ConfirmDeployQA",
            "DeployQA",
            "SmokeTestQA",

            // 预生产
            "ConfirmDeployPreProd",
            "DeployPreProd",
            "SmokeTestPreProd",

            "ConfirmRegressionTestOK",
            "RegressionTestPreProd"

//            "部署预生产环境",
            // DeployProd
//            "ConfirmDeployProd",
//            "AccessControlProd",
//            "DeployProd",
//            "ConfirmDeployOK",
//            "UndoDeployStage",
//            "AutoMergeStage"
        ])
    }

    def wholeFlowWithDev2(project) {
        project.pipeline("WHOLE_FLOW_WITH_DEV2", [
//            "AccessControlDev",
            // Build
            "UploadTestImage",
            "UploadProdImage",
//            "部署Dev环境",
            "DeployDev",
            // DeployQA
            "ConfirmDeployQA",
            "DeployQA",
            "ConfirmDeployPreProd",
            "DeployPreProd",
//            "部署预生产环境",
            // DeployProd
//            "ConfirmDeployProd",
//            "AccessControlProd",
//            "DeployProd",
//            "ConfirmDeployOK",
//            "UndoDeployStage",
//            "AutoMergeStage"
        ])
    }

    def fixFlow(project) {
        project.pipeline("FIX_FLOW", [
//                "AccessControlDev",
                // Build
                "UploadTestImage",
                "UploadProdImage",
//            "部署Dev环境",
                "DeployKmosDev",

            // 预生产
            "ConfirmDeployPreProd",
            "DeployPreProd",
            "SmokeTestPreProd",

            "ConfirmRegressionTestOK",
            "RegressionTestPreProd"
            
//            "部署预生产环境",
                // DeployProd
//            "ConfirmDeployProd",
//            "AccessControlProd",
//            "DeployProd",
//            "ConfirmDeployOK",
//            "UndoDeployStage",
//            "AutoMergeStage"
        ])
    }
    def deployKmosDevFlow(project) {
        project.pipeline("DEPLOY_KMOS_DEV", [
			"AccessControlDev",
            // Build
            "UploadProdImage",
            // DeployDev
            "DeployKmosDev",
            // 冒烟测试
            "SmokeTestDev"
        ])
    }

    def deployDevFlow(project) {
        project.pipeline("DEPLOY_Dev", [
			"AccessControlDev",
            // Build
            "UploadTestImage",
            // DeployDev
            "DeployDev"
        ])
    }

    def deployQAFlow(project) {
        project.pipeline("DEPLOY_QA", [
			"AccessControlQA",
            // Build
            "UploadTestImage",
            // DeployDev
            "部署Dev环境",
            // DeployQA
            "DeployQA",

            // 冒烟测试
            "SmokeTestQA"
        ])
    }

    def deployQA2Flow(project) {
        project.pipeline("DEPLOY_QA2", [
			"AccessControlQA",
            // Build
            "UploadTest2Image",
            // DeployDev
            "部署Dev环境",
            // DeployQA
            "DeployQA2",

            // 冒烟测试
            // "SmokeTestQA"
        ])
    }
    def deployPreProdFlow(project) {
        project.pipeline("DEPLOY_PreProd", [
			"AccessControlPreProd",
            // Build
            "UploadProdImage",
            // DeployDev
            "部署Dev环境",
            // DeployQA
            "部署QA环境",
            // DeployPreProd
            // "ConfirmDeployPreProd",
            "DeployPreProd"
        ])
    }

    def deployProdFlow(project) {
        project.pipeline("DEPLOY_PROD", [
            "MasterCheck",
            "AccessControlProd",
            // Build
            "UploadProdImage",
            // DeployDev
            "部署Dev环境",
            // DeployQA
            "部署QA环境",
            // DeployPreProd
            "确认部署预生产",
			"权限校验",
            "部署预生产环境",
            // DeployProd
            "ConfirmDeployProd",
            "DeployProd",
            "ConfirmDeployOK",
            "UndoDeployStage",
            "AutoMergeStage"
        ])
    }

    def registerFlows(project) {
        this.wholeFlow(project);
        this.wholeFlowWithDev2(project);
        this.deployDevFlow(project);
        this.deployKmosDevFlow(project);
        this.deployQAFlow(project);
        this.deployQA2Flow(project);
        this.deployPreProdFlow(project);
        this.deployProdFlow(project);
        this.fixFlow(project);
    }

    def registerStages(project) {
        // Build
        project.stage("UploadTestImage", new BuildImageStage(project,'构建开发测试上传镜像', 'test'))
        project.stage("UploadTest2Image", new BuildImageStage(project,'构建开发测试上传镜像', 'test2'))
        project.stage("UploadProdImage", new BuildImageStage(project,'构建预发生产上传镜像', 'prod'))
        // Deploy QA
        project.stage("DeployKmosDev", new KmosDevDeployStage(project, '部署Dev环境', true))
        
        // Deploy Dev
        project.stage("ConfirmDeployDev", new ConfirmMessgerStage(project, '确认部署Dev',"部署分支${project.env.BRANCH_NAME}到Dev环境?"))
        project.stage("AccessControlDev", new AccessControlStage(project, '权限校验', 'dev'))
        project.stage("DeployDev", new DevDeployStage(project, '部署Dev环境', true))
        project.stage("SmokeTestDev", new FastRunnerAPITestStage(project, '冒烟测试Dev', 'dev', 1)) //冒烟测试

        // Deploy QA
        project.stage("ConfirmDeployQA", new ConfirmMessgerStage(project, '确认部署QA',"部署分支${project.env.BRANCH_NAME}到QA环境?"))
        project.stage("AccessControlQA", new AccessControlStage(project, '权限校验', 'test'))
        project.stage("DeployQA", new QADeployStage(project, '部署QA环境', true))
        project.stage("DeployQA2", new QA2DeployStage(project, '部署QA2环境', true))
        project.stage("SmokeTestQA", new FastRunnerAPITestStage(project, '冒烟测试QA', 'test', 1)) //冒烟测试

        // DeployToPreProd
        project.stage("ConfirmDeployPreProd", new ConfirmMessgerStage(project, '确认部署预生产',"部署分支${project.env.BRANCH_NAME}到预生产环境?"))
        project.stage("AccessControlPreProd", new AccessControlStage(project, '权限校验', 'pre'))
        project.stage("DeployPreProd", new PreDeployStage(project, '部署预生产', false))
        project.stage("SmokeTestPreProd", new FastRunnerAPITestStage(project, '冒烟测试预生产', 'pre', 1)) //冒烟测试

        project.stage("ConfirmRegressionTestOK", new ConfirmMessgerStage(project, '确认回归测试',"确认执行自动接口回归测试?"))
        project.stage("RegressionTestPreProd", new FastRunnerAPITestStage(project, '回归测试预生产', 'pre', 2)) //回归测试

        // DeployToProd
        project.stage("ConfirmDeployProd", new ConfirmMessgerStage(project, '确认部署生产',"部署分支${project.env.BRANCH_NAME}到生产环境?"))
        project.stage("AccessControlProd", new AccessControlStage(project, '权限校验', 'prod'))
        project.stage("DeployProd", new ProdDeployStage(project, '部署生产环境', true))
        // Test & Merge
        project.stage("ConfirmSmokeTestOK", new ConfirmMessgerStage(project, '冒烟测试',"分支${project.env.BRANCH_NAME}冒烟测试通过?"))
        project.stage("AutoMergeStage",new AutoMergeStage(project, '合并分支'))
        // ok or undo is a question
        project.stage("ConfirmDeployOK", new ConfirmDeployOKStage(project, '上线结果',"prod"))
        project.stage("UndoDeployStage",new UndoDeployStage(project, '回滚版本', 'prod'))
        //判断上线分支是否包含最新代码
        project.stage("MasterCheck", new MasterCheckStage(project,'检测上线分支',"prod"))
    }

    def apply(Project project) {
        project.log "apply flow plugin"

        this.registerFlows(project);
        this.registerStages(project);

        project.log "apply flow plugin ok!"
    }
}
