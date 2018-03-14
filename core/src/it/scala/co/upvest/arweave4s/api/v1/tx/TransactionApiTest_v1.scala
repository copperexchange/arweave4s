package co.upvest.arweave4s.api.v1.tx

import co.upvest.arweave4s.adt.Transaction
import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec}

class TransactionApiTest_v1 extends WordSpec with Matchers with MarshallerV1 {

  import co.upvest.arweave4s.adt._
  import co.upvest.arweave4s.api.ApiTestUtil._
  import io.circe.parser._

  "v1 of the transaction API, on simple backend " when {

    implicit val backend = HttpURLConnectionBackend()

    val transactionId =
      Transaction.Id.fromEncoded("FDqgPohAc15sR0nZjtSo45fa1bzA6kcrigVtw4vSGIM")

    "asked for a full Tx to TxId" should {
      "return a valid Transaction" in {

        val response = tx.getTxViaId(TestHost, transactionId).send()
        // Server should respond OK
        response.statusText shouldBe "OK"
        // Server should respond with Content
        response.body.isRight shouldBe true

        val json = parse(response.body.right.get)
        // Should be a valid JSON
        json.isRight shouldBe true
        // Should be a valid JSON list
        // should be a valid list of peers
        json.flatMap(_.as[Signed[Transaction]]).isRight shouldBe true
      }

      "return tx fields by filter" in {
        val response = tx.getTxViaId(TestHost, transactionId).send()
        val json     = parse(response.body.right.get)
        val transaction = json
          .flatMap(_.as[Signed[Transaction]])
          .getOrElse(throw new IllegalStateException("Could not fetch tx"))

        val filteredResponse = tx.getFilteredTxViaId(TestHost, transactionId, "id").send()
        val id = Transaction.Id.fromEncoded(filteredResponse.body.right.get)

        id shouldBe transaction.t.id
      }

      "return tx data body as HTML" in {
        val response = tx.getBodyAsHtml(TestHost, transactionId).send()
        response.statusText shouldBe "OK"
        // Should be tests with an transaction with non empty data.
      }

      "submitting an valid transaction" in {}
    }
  }
}
