workspace "My-weather-app" "A simple yet powerful weather app with a database and an HTTP controller that returns cool JSON data."{
    model {
        u = person "User"
        ss = softwareSystem "Software System" {
            wa = container "Web Application" {
                cont = component "Controllers"
                dba = component "Database Access"
                serviceLayer = component "Business logic"
            }
            db = container "Database Schema" {
                tags "Database"
            }
        }

        wa -> db "Reads from and writes to"
        serviceLayer -> dba "Converts raw database output to easy-to-read JSON"
        cont -> u "Send and receive HTTP traffic"
        cont -> serviceLayer "Calls functions corresponding to user requests"
        dba -> db "Compiles and executes queries"

    }

    views {
        systemContext ss "Diagram1" {
            include *
            autolayout lr
        }

        container ss "Diagram2" {
            include *
            autolayout lr
        }

        component wa "Diagram3"{
            include *
            autolayout tb
        }


        styles {
            element "Person" {
                shape person
            }
            element "Database" {
                shape cylinder
            }
        }
    }
}