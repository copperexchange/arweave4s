package co.upvest.arweave4s.api.v1.tx

import co.upvest.arweave4s.api.v1.marshalling.MarshallerV1
import com.softwaremill.sttp.HttpURLConnectionBackend
import org.scalatest.{Matchers, WordSpec, Inside}

import scala.util.Random

class TransactionApiTest_v1 extends WordSpec with Matchers with MarshallerV1 with Inside {

  import co.upvest.arweave4s.adt._
  import co.upvest.arweave4s.api.ApiTestUtil._
  import io.circe.parser._
  import io.circe.syntax._

  "v1 of the transaction API, on simple backend " when {

    implicit val backend = HttpURLConnectionBackend()

    val Some(transactionId) = Transaction.Id.fromEncoded(
      "3MFrfH0-HI9GeMfFAwIhK9TcASsxDJeK6MFMbJplSkU"
    )

    "asked for a full Tx to TxId" should {
      "return a valid Transaction" in {
        val response = tx.getTxViaId(TestHost, transactionId.toString).send()
        response.code shouldBe 200

        inside(response.body) {
          case Right(body) =>
            inside(parse(body) flatMap { _.as[Signed[Transaction]] }) {
              case Right(stx) =>
                stx.verify(stx.t.owner) shouldBe true
            }
        }
      }

      "return tx fields by filter" in {
        val response = tx.getTxViaId(TestHost, transactionId.toString).send()
        val json     = parse(response.body.right.get)
        val transaction = json
          .flatMap(_.as[Signed[Transaction]])
          .getOrElse(throw new IllegalStateException("Could not fetch tx"))

        val filteredResponse = tx.getFilteredTxViaId(TestHost, transactionId.toString, "id").send()
        val Some(id)         = Transaction.Id.fromEncoded(filteredResponse.body.right.get)

        id shouldBe transaction.t.id
      }

      "return tx data body as HTML" in {
        val response = tx.getBodyAsHtml(TestHost, transactionId.toString).send()
        response.statusText shouldBe "OK"
        // Should be tests with an transaction with non empty data.
      }

      "submitting a valid transfer transaction" in {
        pending
        val foobar = Wallet.generate()

        val stx = Transaction
          .Transfer(
            Transaction.Id.generate(),
            None, // TODO: why is the transaction accepted even when the account has a last_tx?
            TestAccount.wallet,
            foobar.address,
            quantity = Winston("1000"),
            reward = Winston("100")
          )
          .sign(TestAccount.wallet)

        tx.postTx(TestHost, stx.asJson.noSpaces).send().code shouldBe 200
      }

      "submitting a valid data transaction" in {
        pending

        val foobar = Wallet.generate()

        val stx = Transaction
          .Data(Transaction.Id.generate(), None, TestAccount.wallet, new Data(Random.nextString(100).getBytes), reward = Winston("100"))
          .sign(TestAccount.wallet)

        tx.postTx(TestHost, stx.asJson.noSpaces).send().code shouldBe 200
      }
    }
  }
}
