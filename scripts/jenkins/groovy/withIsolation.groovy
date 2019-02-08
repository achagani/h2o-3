def call() {
    IsolationProvider result =  new IsolationProvider()
    result.initialize(this)
    return result
}

class IsolationProvider {
    
    private insideDockerScript
    private insidePod
    
    def initialize(final context) {
        insideDockerScript = context.load('h2o-3/scripts/jenkins/groovy/insideDocker.groovy')
        insidePod = context.load('h2o-3/scripts/jenkins/groovy/insidePod.groovy')
    }

    def withIsolation(final context, final isolationType, final args, final body) {
        switch (isolationType) {
            case 'docker':
                context.echo "Running in docker container with args: ${args}"
                insideDockerScript.call(args.customEnv, args.image, args.registry, args.buildConfig, args.timeoutValue, 'MINUTES', args.customDockerArgs, body)
                break
            case 'pod':
                withDefaultWrappers(context, args.customEnv, args.timeoutValue) {
                    context.echo "Running in k8s pod with args: ${args}"
                    insidePod.call(args.mem, args.cpu, args.image, body)
                }
                break
            case 'none':
                withDefaultWrappers(context, args.customEnv, args.timeoutValue) {
                    context.sh """
                        id
                        printenv | sort
                    """
                    context.echo "Running without additional isolation"
                    context.withCredentials([
                            context.file(credentialsId: 'c096a055-bb45-4dac-ba5e-10e6e470f37e', variable: 'JUNIT_CORE_SITE_PATH'),
                            [$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'AWS S3 Credentials', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']
                    ]) {
                        body()
                    }
                }
                break
            default:
                error "Isolation type ${isolationType} not supported"
        }
    }

    private withDefaultWrappers(final context, final customEnv, final timeoutValue, final Closure body) {
        context.withEnv(customEnv) {
            context.timeout(time: timeoutValue, unit: 'MINUTES') {
                body()
            }
        }
    }
}

return this
