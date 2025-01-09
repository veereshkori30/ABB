@Library('sm-jenkins-shared-library@api') _
pipeline {
    agent {
        label "slave-1"
    }
    environment {
        IMAGE_REPO_NAME = "api/multichannel_whatsapp"
        PROJECT_NAME = 'multichannel-whatsapp'
        ECS_CLUSTER_NAME = 'api-cluster-ecs'
        AWS_MAX_ATTEMPTS = '5'
    }
    options {
        buildDiscarder(
            logRotator(
                numToKeepStr: '7',
                daysToKeepStr: '10',
            )
        )
    }

    stages { // This stage will executed only if tag is v* and this will ask input only for prod deployment ( as we deploy in prod using tag v* )
        stage('User Input stage'){
            when { tag 'v*' }
            steps {
                script {
                    env.ENVIRONMENT = input(message: 'Please choose the deployment env',
                    parameters: [
                        choice(
                            choices: ['prod-aus', 'prod-eu', 'prod-us'],
                            description: '',
                            name: 'Environment'
                        )
                    ])
                }
            }
        }
        // This stage will be executed only if branch is qa
        // In this stage we are defining the env variables for deployment in QA ENV
        stage('QA Env') {
            when {
                anyOf {
                    branch "qa"
                    branch "qa-*"
                    }
                }
            steps {
                script {
                    set_QA_Constant_Env()

                    env.ECS_SERVICES = '''
                        multichannel_whatsapp_svc
                    '''
                    env.TASK_DEFINITION_FILE_NAMES = '''
                        multichannel_whatsapp_td
                    '''
                    env.TASK_DEFINITION_FILE_PATHS = '''
                        task-definitions/qa/multichannel_whatsapp.json
                    '''
                    env.TARGET_GROUP_ARN = '''
                        arn:aws:elasticloadbalancing:us-east-1:454947665516:targetgroup/sm-TG-qa-multichannel-api/3b4974543521f69c
                    '''
                }
            }
        }
        // This stage will be executed if branch is integration
        stage('Integration Env') {
            when {
                anyOf {
                    branch "integration"
                    branch "intg-*"
                }
            }
            steps {
                script {
                    set_Integration_Constant_Env()
                    env.ECS_SERVICES = '''
                        multichannel_whatsapp_svc
                    '''
                    // use family name defined in task_definition.json
                    env.TASK_DEFINITION_FILE_NAMES = '''
                        multichannel_whatsapp_td
                    '''
                    env.TASK_DEFINITION_FILE_PATHS = '''
                        task-definitions/integration/multichannel_whatsapp.json
                    '''
                    env.TARGET_GROUP_ARN = '''
                        arn:aws:elasticloadbalancing:us-east-1:134746102940:targetgroup/sm-tg-multi-whatsapp-message/1df69b13fbce995b
                    '''
                }
            }
        }
        // This stage will be executed if tag is rc-* ( we deploy in staging using tag rc-* )
        stage('Staging Env') {
            when {
                anyOf {
                    tag 'rc-*'
                    branch "staging-*"
                }
                }
            steps {
                script {
                    set_Staging_Constant_Env()

                    env.ECS_SERVICES = '''
                        multichannel_whatsapp_svc
                    '''
                    env.TASK_DEFINITION_FILE_NAMES = '''
                        multichannel_whatsapp_td
                    '''
                    env.TASK_DEFINITION_FILE_PATHS = '''
                        task-definitions/staging/multichannel_whatsapp.json
                    '''
                    env.TARGET_GROUP_ARN = '''
                        arn:aws:elasticloadbalancing:us-east-1:346900201582:targetgroup/sm-tg-multi-messages-upstream-1/0dbc964e95fe0ce3
                    '''
                }
            }
        }
        // This stage will be executed if tag is v* and you have selected ENVIRONMENT as prod-us in first stage
        stage('US Env') {
            when {
                expression {env.ENVIRONMENT == 'prod-us'}
            }
            steps {
                script {
                    set_Prod_US_Constant_Env()

                    env.ECS_SERVICES = '''
                        multichannel_whatsapp_svc
                    '''
                    env.TASK_DEFINITION_FILE_NAMES = '''
                        multichannel_whatsapp_td
                    '''
                    env.TASK_DEFINITION_FILE_PATHS = '''
                        task-definitions/prod-us/multichannel_whatsapp.json
                    '''
                    env.TARGET_GROUP_ARN = '''
                        arn:aws:elasticloadbalancing:us-east-1:725539715953:targetgroup/sm-tg-messages-upstream/11f2eca370b8f5e2
                    '''
                }
            }
        }
        // This stage will be executed if tag is v* and you have selected ENVIRONMENT as prod-aus in first stage
        stage('AUS Env') {
            when {
                expression {env.ENVIRONMENT == 'prod-aus'}
            }
            steps {
                script {
                    set_Prod_AUS_Constant_Env()

                    env.ECS_SERVICES = '''
                        multichannel_whatsapp_svc
                    '''
                    env.TASK_DEFINITION_FILE_NAMES = '''
                        multichannel_whatsapp_td
                    '''
                    env.TASK_DEFINITION_FILE_PATHS = '''
                        task-definitions/prod-aus/multichannel_whatsapp.json
                    '''
                    env.TARGET_GROUP_ARN = '''
                        arn:aws:elasticloadbalancing:ap-southeast-2:514231095641:targetgroup/sm-TG-aus-multichannel-api/c39dae6ed55a78e9
                    '''
                }
            }
        }
        // This stage will be executed if tag is v* and you have selected ENVIRONMENT as prod-eu in first stage
        stage('EU Env') {
            when {
                expression {env.ENVIRONMENT == 'prod-eu'}
            }
            steps {
                script {
                    set_Prod_EU_Constant_Env()

                    env.ECS_SERVICES = '''
                        multichannel_whatsapp_svc
                    '''
                    env.TASK_DEFINITION_FILE_NAMES = '''
                        multichannel_whatsapp_td
                    '''
                    env.TASK_DEFINITION_FILE_PATHS = '''
                        task-definitions/prod-eu/multichannel_whatsapp.json
                    '''
                    env.TARGET_GROUP_ARN = '''
                        arn:aws:elasticloadbalancing:eu-west-1:121234700868:targetgroup/sm-sg-api-messages-upstream/590046668d7fab2b
                    '''
                }
            }
        }
        stage('Build and tag docker image') {
            steps {
                script {
                    BuildAndTagDockerImage()
                }
            }
        }
        // This stage will create ECR Repository if not already exists
        stage('Create ECR Repository') {
            steps {
                script {
                    withAWS(region: "$AWS_DEFAULT_REGION", role: "$ROLE_ARN") {
                        CreateECRRepoIfNotExists()
                    }
                }
            }
        }
        // This stage will be used to login into the ecr repository
        stage('AWS ECR login') {
            steps {
                script {
                    withAWS(region: "$AWS_DEFAULT_REGION", role: "$ROLE_ARN") {
                        LoginAwsEcrRepo()
                    }
                }
            }
        }
        stage('Deploy') {
            steps {
                script {
                    Deploy()
                }
            }
        }
        stage('Pushing build image to ECR') {
            steps {
                script {
                    withAWS(region: "$AWS_DEFAULT_REGION", role: "$ROLE_ARN") {
                        PushImageToECR()
                    }
                }
            }
        }
        stage('Register ecs-task-definition.json') {
            steps {
                script {
                    withAWS(region: "$AWS_DEFAULT_REGION", role: "$ROLE_ARN") {
                        RegisterECSTaskDef()
                    }
                }
            }
        }
        stage('Modify port of target group to traffic port if it is not already') {
            steps {
                script {
                    withAWS(region: "$AWS_DEFAULT_REGION", role: "$ROLE_ARN") {
                        ModifyPortToTrafficPort()
                    }
                }
            }
        }
        // In this stage we will create the target group, alb listener rule , and will created ecs service using cloudformation
        // We have used the cloudformation deploy which will try to create the stack if it exists, it will try to update if anything changes in cloudformation.yaml or parameters.yaml
        stage('deploy cloudformation stack') {
            steps {
                script {
                    withAWS(region: "${AWS_DEFAULT_REGION}", role: "${ROLE_ARN}") {
                        sh "aws cloudformation delete-stack --stack-name ${env.PROJECT_NAME}-${env.ENV}"
                        DeployCloudFormationStack()
                    }
                }
            }
        }
        stage('ECS tasks autoscaling for prod-env') {
            when {
                expression {env.ENVIRONMENT == 'prod-aus' || env.ENVIRONMENT == 'prod-eu' || env.ENVIRONMENT == 'prod-us'}
                }
                steps {
                    script {
                        withAWS(region: "${AWS_DEFAULT_REGION}", role: "${ROLE_ARN}") {
                            EcsTasksProdAutoscaling()
                        }
                    }
                }
        }
    }

    post {
        always {
            NotifyBuildStatus(currentBuild.currentResult)
            cleanWs deleteDirs: true
        }
    }

}