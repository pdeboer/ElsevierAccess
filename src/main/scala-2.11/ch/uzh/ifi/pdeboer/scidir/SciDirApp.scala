package ch.uzh.ifi.pdeboer.scidir

import java.io.{InputStreamReader, BufferedReader, FileOutputStream, File}
import java.net.URL

import com.github.tototoshi.csv.{CSVWriter, CSVReader}
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.{HttpResponse, HttpRequest}
import org.apache.http.client.RedirectStrategy
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{HttpUriRequest, HttpGet}
import org.apache.http.impl.client.{DefaultRedirectStrategy, HttpClientBuilder}
import org.apache.http.protocol.{HttpContext, HttpRequestExecutor, BasicHttpContext}
import org.apache.http.util.EntityUtils

import scala.sys.process._


/**
 * Created by mattia on 18.09.15.
 */
object SciDirApp extends App with LazyLogging{

  val reader = CSVReader.open(new File("./download/restrictedAccess.csv"))
  val PIIs = reader.all().map(line => line(0))

  val httpClient = HttpClientBuilder.create()
    .setUserAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.93 Safari/537.36")
    .build()

  val httpContext = new BasicHttpContext()

  val writer = CSVWriter.open(new File("./download/linkMissing.csv"))

  val hrefTag = "pdfurl=\""

  PIIs.foreach(pii => {

    val content = get("http://www.sciencedirect.com/science/article/pii/" + pii)
    val startIndex = content.indexOf(hrefTag) + hrefTag.length

    val link = content.substring(startIndex, content.indexOf(".pdf", startIndex) + 4)
    try {
      val exec =
        "wget -O " + new File("download/").getAbsolutePath + "/" + pii + ".pdf " + link + " --user-agent='Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.93 Safari/537.36' --header='Content-Type: text/plain; charset=utf-8' --header='Accept: /' --header='Accept-Encoding: gzip,deflate,sdch' --header='Accept-Language: en-US,en;q=0.8,de;q=0.6'" !!
    }catch {
      case e: Exception => e.printStackTrace()
    }
    writer.writeRow(Seq[String](link))
    Thread.sleep(10000)
  })
  writer.close()


  def get(url: String): String = {
    logger.info(s"Executing query: $url")
    val request = new HttpGet(url)
    val response = httpClient.execute(request, httpContext).getEntity
    val content = EntityUtils.toString(response)
    EntityUtils.consumeQuietly(response)
    content
  }

}
