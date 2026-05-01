workspace "CoActivity - C4" "C4 context and component diagrams" {

  !identifiers hierarchical

  model {
    guest = person "Guest" "Browses public rooms and Q&A without authentication." {
      tags "Guest"
    }
    user = person "Authorized User" "Uses full platform features after login." {
      tags "User"
    }

    smtp = softwareSystem "SMTP Provider" "External email provider." {
      tags "External"
    }

    coactivity = softwareSystem "CoActivity" "Platform for rooms, Q&A and notifications." {
      tags "ContextSystem"

      web = container "Web Application" "" "React/Vite" {
        tags "WebApp"
      }
      core = container "Core Service" "" "Spring Boot, Java" {
        tags "Backend"

        group "API Controllers" {
          apiControllers = component "API Controllers" "" "Spring MVC REST Controllers" {
            tags "Controller"
          }
        }

        group "Services" {
          servicesLayer = component "Services" "" "Spring Services" {
            tags "Service"
          }
        }

        group "Repository" {
          repositoryLayer = component "Repository" "" "Spring Data JPA" {
            tags "Repository"
          }
        }
      }
      notifications = container "Mail Service" "" "notification-service" {
        tags "MailService"
      }

      postgres = container "Database" "" "PostgreSQL 16" {
        tags "Database"
      }
      redis = container "Redis" "" "Session Store" {
        tags "RedisStore"
      }
      kafka = container "Kafka" "" "Message Broker" {
        tags "MessageBroker"
      }
      storage = container "Object Storage" "" "S3/MinIO" {
        tags "ObjectStorage"
      }

    }

    guest -> coactivity "Public" "HTTPS" {
      tags "ContextRelationship"
    }
    user -> coactivity "All features" "HTTPS" {
      tags "ContextRelationship"
    }

    coactivity -> smtp "Email" "SMTP" {
      tags "ContextRelationship"
    }

    coactivity.web -> coactivity.core "REST API" "HTTPS"
    coactivity.core -> coactivity.postgres "Data" "JDBC"
    coactivity.core -> coactivity.redis "Sessions" "Redis"
    coactivity.core -> coactivity.storage "Files" "S3 API"
    coactivity.core -> coactivity.kafka "Events" "Kafka"
    coactivity.kafka -> coactivity.notifications "Email events" "Kafka"
    coactivity.notifications -> smtp "Email" "SMTP"
  }

  views {

    systemContext coactivity {
      include guest
      include user
      include coactivity
      include smtp
      autolayout lr 200 100
      title "System Context"
    }

    container coactivity "ComponentDiagram" {
      include coactivity.web
      include coactivity.core
      include coactivity.notifications
      include coactivity.redis
      include coactivity.kafka
      include coactivity.postgres
      include coactivity.storage
      include smtp
      title "Component Diagram"
    }

    styles {

      element "*" {
        fontSize 36
      }

      element "Group" {
        color #E30611
      }

      element "Person" {
        shape person
        background #E30611
        color #ffffff
        fontSize 40
        metadata false
        description false
      }

      element "Guest" {
        background #AAAAAA
        fontSize 40
      }

      element "User" {
        background #E30611
        fontSize 40
      }

      element "Software System" {
        background #E30611
        color #ffffff
        fontSize 40
      }

      element "ContextSystem" {
        background #E30611
        color #ffffff
        fontSize 40
        metadata false
        description false
      }

      element "Container" {
        background #C41E3A
        color #ffffff
        fontSize 40
        metadata false
        description false
      }

      element "WebApp" {
        shape webBrowser
        background #FF6B6B
        fontSize 40
        width 420
      }

      element "Backend" {
        shape roundedBox
        background #C41E3A
        fontSize 40
        width 460
      }

      element "MailService" {
        shape roundedBox
        background #C41E3A
        color #ffffff
        fontSize 40
        width 360
      }

      element "Database" {
        shape cylinder
        background #B22222
        color #ffffff
        width 320
        fontSize 40
      }

      element "MessageBroker" {
        shape pipe
        background #B22222
        color #ffffff
        width 320
        fontSize 40
      }

      element "RedisStore" {
        shape cylinder
        background #B22222
        color #ffffff
        width 280
        fontSize 40
      }

      element "ObjectStorage" {
        shape cylinder
        background #B22222
        color #ffffff
        width 320
        fontSize 40
      }

      element "Component" {
        background #FFE4E4
        color #000000
        fontSize 40
      }

      element "Controller" {
        shape hexagon
        background #FFCCCC
        fontSize 40
      }

      element "Service" {
        shape roundedBox
        background #FFE4E4
        fontSize 40
      }

      element "Repository" {
        shape cylinder
        background #FFCCCC
        width 500
        fontSize 40
      }

      element "External" {
        background #888888
        color #ffffff
        fontSize 40
        width 480
        metadata false
        description false
      }

      relationship "ContextRelationship" {
        thickness 2
        color #333333
        fontSize 40
        width 140
      }

      relationship "Relationship" {
        thickness 2
        color #333333
        fontSize 40
      }
    }

    theme default
  }
}
