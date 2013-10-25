package com.outr.net.examples

import com.outr.net.http.WebApplication
import com.outr.net.communicator.server.{PongResponder, Communicator}
import org.powerscala.log.Logging
import com.outr.net.http.session.MapSession
import com.outr.net.http.request.HttpRequest
import com.outr.net.http.handler.{MultipartHandler, CachedHandler}
import com.outr.net.http.jetty.JettyApplication
import com.outr.net.http.response.{HttpResponseStatus, HttpResponse}
import java.io.File
import com.outr.net.http.content.StringContent

/**
 * @author Matt Hicks <matt@outr.com>
 */
object ExampleWebApplication extends WebApplication[MapSession] with Logging with JettyApplication {
  PongResponder.connect()     // Support ping-pong
  TimeResponder.connect()     // Support time request

  protected def createSession(request: HttpRequest, id: String) = new MapSession(id)

  def init() = {
    handlers += CachedHandler     // Add caching support
    Communicator.configure(this)

    addHandler(new MultipartHandler {
      def onField(name: String, value: String) = println(s"onField! $name = $value")

      def onFile(filename: String, file: File) = println(s"onFile! $filename - ${file.length()}")

      def finish(request: HttpRequest, response: HttpResponse) = {
        response.copy(status = HttpResponseStatus.OK, content = StringContent("Received files!"))
      }
    }, "/uploader")

    // Add example html files
    addClassPath("/", "html/")

    Communicator.created.on {
      case (connection, data) => {
        info(s"Created! ${connection.id} - $data")
        connection.received.on {
          case message => info(s"Message Received: $message")
        }
      }
    }
    Communicator.connected.on {
      case (connection, data) => {
        info(s"Connected! ${connection.id} - $data")
      }
    }
    Communicator.disconnected.on {
      case connection => {
        info(s"Disconnected! ${connection.id}")
      }
    }
    Communicator.disposed.on {
      case connection => {
        info(s"Disposed! ${connection.id}")
      }
    }
  }

  override def dispose() = {
    super.dispose()
    info("Disposed application!")
  }
}
