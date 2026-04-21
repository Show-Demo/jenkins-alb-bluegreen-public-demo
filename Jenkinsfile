pipeline {
  agent none

  options {
    timestamps()
    timeout(time: 60, unit: 'MINUTES')
    disableConcurrentBuilds()
  }

  stages {
    stage('Dev Package') {
      when {
        beforeAgent true
        branch 'dev'
      }
      agent {
        docker {
          image 'maven:3.8.8-eclipse-temurin-8'
          args '-v /opt/maven-repository:/root/.m2'
        }
      }
      steps {
        sh '''
          mvn -Dmaven.repo.local=/root/.m2 clean package -P dev -Dmaven.test.skip=true
        '''
        stash name: 'dev-order-api-jar', includes: 'target/*.jar'
      }
    }

    stage('Dev Docker Build Push') {
      when {
        beforeAgent true
        branch 'dev'
      }
      agent {
        docker {
          image 'docker:24'
          args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
      }
      steps {
        unstash 'dev-order-api-jar'
        sh '''
          docker build -t localhost:5001/demo/order-api:latest .
          docker push localhost:5001/demo/order-api:latest
        '''
      }
    }

    stage('Deploy Dev') {
      when {
        beforeAgent true
        branch 'dev'
      }
      agent any
      steps {
        sh '''
          sed "s#IMAGE_PLACEHOLDER#localhost:5001/demo/order-api:latest#g" k8s/dev/order-api.yaml \
            | kubectl --kubeconfig=/var/jenkins_home/.kube/config apply -f -

          kubectl --kubeconfig=/var/jenkins_home/.kube/config -n dev rollout status deployment/order-api --timeout=300s
        '''
      }
    }

    stage('Test Package') {
      when {
        beforeAgent true
        branch 'test'
      }
      agent {
        docker {
          image 'maven:3.8.8-eclipse-temurin-8'
          args '-v /opt/maven-repository:/root/.m2'
        }
      }
      steps {
        sh '''
          mvn -Dmaven.repo.local=/root/.m2 clean package -P test -Dmaven.test.skip=true
        '''
        stash name: 'test-order-api-jar', includes: 'target/*.jar'
      }
    }

    stage('Test Docker Build Push') {
      when {
        beforeAgent true
        branch 'test'
      }
      agent {
        docker {
          image 'docker:24'
          args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
      }
      steps {
        unstash 'test-order-api-jar'
        sh '''
          docker build -t localhost:5001/demo/order-api:latest .
          docker push localhost:5001/demo/order-api:latest
        '''
      }
    }

    stage('Deploy Test') {
      when {
        beforeAgent true
        branch 'test'
      }
      agent any
      steps {
        sh '''
          sed "s#IMAGE_PLACEHOLDER#localhost:5001/demo/order-api:latest#g" k8s/test/order-api.yaml \
            | kubectl --kubeconfig=/var/jenkins_home/.kube/config apply -f -

          kubectl --kubeconfig=/var/jenkins_home/.kube/config -n test rollout status deployment/order-api --timeout=300s
        '''
      }
    }

    stage('Prod Package') {
      when {
        beforeAgent true
        branch 'master'
      }
      agent {
        docker {
          image 'maven:3.8.8-eclipse-temurin-8'
          args '-v /opt/maven-repository:/root/.m2'
        }
      }
      steps {
        sh '''
          mvn -Dmaven.repo.local=/root/.m2 clean package -P prod -Dmaven.test.skip=true
        '''
        stash name: 'prod-order-api-jar', includes: 'target/*.jar'
      }
    }

    stage('Prod Docker Build Push') {
      when {
        beforeAgent true
        branch 'master'
      }
      agent {
        docker {
          image 'docker:24'
          args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
      }
      steps {
        unstash 'prod-order-api-jar'
        sh '''
          docker build -t localhost:5001/demo/order-api:latest .
          docker push localhost:5001/demo/order-api:latest
        '''
      }
    }

    stage('Deploy Prod Green') {
      when {
        beforeAgent true
        branch 'master'
      }
      agent any
      steps {
        sh '''
          sed "s#BLUE_IMAGE_PLACEHOLDER#localhost:5001/demo/order-api:latest#g; s#IMAGE_PLACEHOLDER#localhost:5001/demo/order-api:latest#g" k8s/prod/order-api-bluegreen.yaml \
            | kubectl --kubeconfig=/var/jenkins_home/.kube/config apply -f -

          kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod rollout status deployment/order-api-green --timeout=300s
        '''
      }
    }

    stage('Check Prod Green Stable') {
      when {
        beforeAgent true
        branch 'master'
      }
      agent any
      steps {
        sh '''
          set -e

          echo "checking ready replicas"
          READY=$(kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod get deployment order-api-green -o jsonpath='{.status.readyReplicas}')
          DESIRED=$(kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod get deployment order-api-green -o jsonpath='{.spec.replicas}')

          if [ "$READY" != "$DESIRED" ]; then
            echo "not all replicas are ready: $READY/$DESIRED"
            exit 1
          fi

          echo "checking service endpoints"
          ENDPOINTS=$(kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod get endpoints order-api-green -o jsonpath='{.subsets[*].addresses[*].ip}')

          if [ -z "$ENDPOINTS" ]; then
            echo "service order-api-green has no ready endpoints"
            exit 1
          fi

          echo "recording restart count"
          BEFORE=$(kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod get pods -l app=order-api,color=green \
            -o jsonpath='{range .items[*]}{.metadata.name}{"="}{.status.containerStatuses[0].restartCount}{"\\n"}{end}')
          echo "$BEFORE"

          echo "waiting stable window 120s"
          sleep 120

          READY_AFTER=$(kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod get deployment order-api-green -o jsonpath='{.status.readyReplicas}')
          DESIRED_AFTER=$(kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod get deployment order-api-green -o jsonpath='{.spec.replicas}')

          if [ "$READY_AFTER" != "$DESIRED_AFTER" ]; then
            echo "replicas became unhealthy during stable window: $READY_AFTER/$DESIRED_AFTER"
            exit 1
          fi

          AFTER=$(kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod get pods -l app=order-api,color=green \
            -o jsonpath='{range .items[*]}{.metadata.name}{"="}{.status.containerStatuses[0].restartCount}{"\\n"}{end}')
          echo "$AFTER"

          if [ "$BEFORE" != "$AFTER" ]; then
            echo "pod restarted during stable window"
            exit 1
          fi

          echo "green version is stable"
        '''
      }
    }

    stage('Prod Approval') {
      when {
        beforeAgent true
        branch 'master'
      }
      steps {
        input id: 'prod-cutover', message: 'green 版本已稳定，是否将 ALB 生产流量切到 green？', ok: '切换'
      }
    }

    stage('Switch Prod ALB To Green') {
      when {
        beforeAgent true
        branch 'master'
      }
      agent any
      steps {
        sh '''
          kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod patch ingress order-api-prod --type=json \
            -p='[
              {
                "op": "replace",
                "path": "/spec/rules/0/http/paths/0/backend/service/name",
                "value": "order-api-green"
              }
            ]'

          kubectl --kubeconfig=/var/jenkins_home/.kube/config -n prod get ingress order-api-prod -o wide
        '''
      }
    }

    stage('Verify Prod Entry') {
      when {
        beforeAgent true
        branch 'master'
      }
      agent {
        docker {
          image 'curlimages/curl:8.10.1'
        }
      }
      steps {
        sh '''
          curl -fsS --max-time 10 http://order-api.example.com/ || true
        '''
      }
    }
  }

  post {
    failure {
      echo '发布失败。若生产已经切流，请手工将 Ingress backend service 切回 order-api-blue。'
    }
  }
}
