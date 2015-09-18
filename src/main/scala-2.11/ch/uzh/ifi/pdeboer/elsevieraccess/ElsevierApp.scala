package ch.uzh.ifi.pdeboer.elsevieraccess

import java.io._
import java.net.URI

import com.github.tototoshi.csv.CSVWriter
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.params.ClientPNames
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.{BasicCookieStore, HttpClientBuilder}
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.util.EntityUtils
import play.api.libs.json.{JsValue, Json}

/**
 * Created by mattia on 17.09.15.
 */
object ElsevierApp extends App with LazyLogging{

  emptyDirRecursively(new File("./download/"))

  val config = ConfigFactory.load()

  val key = ConfigFactory.load("apiKey.conf").getString("key")
  logger.debug("Key: " + key)

  val httpClient = HttpClientBuilder.create().build()
  val httpContext = new BasicHttpContext()

  val cookieStore = new BasicCookieStore()
  httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore)

  val writer = CSVWriter.open(new File("./download/restrictedAccess.csv"))

  for(i <- 0 to 21) {
    val answer = Json.parse(get(s"${config.getString("search")}&APIKey=$key&start=${200*i}"))
    (answer \\ "entry").par.foreach(entry => {
      val urls = (entry \\ "pii").map(_.toString().replaceAll("\"", ""))
      //val pubTypes = (entry \\ "pubType").map(_.toString().replaceAll("\"", ""))
      val openAccess = (entry \\ "openaccessArticle").map(_.toString().replaceAll("\"", ""))
      (urls zip openAccess).par.foreach(elem => {
        if (elem._2.equalsIgnoreCase("true")) {
          downloadAndStore(config.getString("getPdf") + elem._1 + s"?httpAccept=application/pdf&apiKey=$key", "./download/" + elem._1 + ".pdf")
        } else if (elem._2.equalsIgnoreCase("false")) {
          this.synchronized {
            writer.writeRow(Seq[String](elem._1, elem._2))
          }
        }
      })
    })
  }

  writer.close()
  httpClient.close()
  httpContext.clear()

  def get(url: String): String = {
    logger.info(s"Executing query: $url")
    val request = new HttpGet(url)
    val response = httpClient.execute(request, httpContext).getEntity
    val content = EntityUtils.toString(response)
    EntityUtils.consumeQuietly(response)
    content
  }

  def downloadAndStore(url: String, filePath: String): Unit = {
    try {
      logger.info(s"Downloading: $url")
      val request = new HttpGet(url)
      val response = httpClient.execute(request, httpContext).getEntity
      val is = response.getContent
      val bos = new FileOutputStream(new File(filePath))
      val content : Array[Byte]= Stream.continually(is.read).takeWhile(-1 !=).map(_.toByte).toArray
      bos.write(content)
      is.close()
      bos.close()

      EntityUtils.consumeQuietly(response)
    }catch{
      case e: Exception => e.printStackTrace()
    }
  }

  def emptyDirRecursively(dir: File): Boolean = {
    dir.listFiles().par.foreach(file => {
      if (file.isDirectory) {
        emptyDirRecursively(file)
      }
      file.delete()
    })
    true
  }
}


