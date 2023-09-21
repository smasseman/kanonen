package se.smasseman.kanonen.web.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*
import se.smasseman.kanonen.util.NetworkUtil
import java.io.File

fun Application.configureRouting() {
    install(Webjars) {
        path = "/webjars" //defaults to /webjars
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        get("/webjars") {
            call.respondText(
                "<script src='/webjars/jquery/jquery.js'></script>",
                ContentType.Text.Html
            )
        }
        get("/") {
            call.respondRedirect("static/index.html")
        }
        get("/static/{file}") {
            call.respondFile(
                File(
                    System.getProperty(
                        "staticfiles",
                        "/Users/jorgensmas/git/smasseman/kanonen/src/main/resources/static"
                    )
                ),
                call.parameters["file"].toString()
            )
        }
        get("/ip") {
            call.respondText { NetworkUtil.getIp() }
        }
        // Static plugin. Try to access `/static/index.html`
        static("/static") {
            resources("static")
        }
    }


}
