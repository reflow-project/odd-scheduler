# odd-scheduler
scheduling component of the odd, which serves the purpose to trigger new data harvesting pipelines

# installation guide with docker

- mvn clean package

- docker build -t odd_scheduler .


_it is highly recommended to only deploy the odd-scheduler in combination with the other ODD-components. The respective docker-compose.yml for this purpose can be found in a separate repository._