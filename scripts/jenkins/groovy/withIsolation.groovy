def call(final isolationType, final args, final body) {
    def insideDocker = load('h2o-3/scripts/jenkins/groovy/insideDocker.groovy')
    def insidePod = load('h2o-3/scripts/jenkins/groovy/insidePod.groovy')

    switch (isolationType) {
        case 'docker':
            echo "Running in docker container with args: ${args}"
            insideDocker(args.customEnv, args.image, args.registry, args.buildConfig, args.timeoutValue, 'MINUTES', args.customDockerArgs, body)
            break
        case 'pod':
            withDefaultWrappers(args.customEnv, args.timeoutValue) {
                echo "Running in k8s pod with args: ${args}"
                insidePod(args.mem, args.cpu, args.image, body)
            }
            break
        case 'none':
            withDefaultWrappers(args.customEnv, args.timeoutValue) {
                echo "Running without additional isolation"
                body()
            }
            break
        default: 
            error "Isolation type ${isolationType} not supported"
    }
}

def withDefaultWrappers(final customEnv, final timeoutValue, final Closure body) {
    withEnv(customEnv) {
        timeout(time: timeoutValue, unit: 'MINUTES') {
            withCredentials([file(credentialsId: 'c096a055-bb45-4dac-ba5e-10e6e470f37e', variable: 'JUNIT_CORE_SITE_PATH'), [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'AWS S3 Credentials', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                sh """
                    id
                    printenv | sort
                """
                body()
            }
        }
    }
}

return this
