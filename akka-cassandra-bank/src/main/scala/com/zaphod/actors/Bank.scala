package com.zaphod.actors

import akka.NotUsed
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}
import akka.util.Timeout

import java.util.UUID
import scala.util.Failure

object Bank {
  import PersistentBankAccount.Command._
  import PersistentBankAccount.Response._
  import PersistentBankAccount.Command

  sealed trait Event
  case class BankAccountCreated(id: String) extends Event

  case class State(accounts: Map[String, ActorRef[Command]])

  def commandHandler(context: ActorContext[Command]): (State, Command) => Effect[Event, State] = (state, command) =>
    command match {
      case createCmd @ CreateBankAccount(_, _, _, _) =>
        val id = UUID.randomUUID().toString
        val newBankAccount = context.spawn(PersistentBankAccount(id), id)
        Effect
        .persist(BankAccountCreated(id))
        .thenReply(newBankAccount)(_ => createCmd)

      case updateCmd @ UpdateBalance(id, _, _, replyTo) =>
        state.accounts.get(id) match {
          case Some(account) =>
            Effect.reply(account)(updateCmd)
          case None =>
            Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(Failure(new RuntimeException("Bank account cannot be found"))))
        }

      case getCmd @ GetBankAccount(id, replyTo) =>
        state.accounts.get(id) match {
          case Some(account) =>
            Effect.reply(account)(getCmd)
          case None =>
            Effect.reply(replyTo)(GetBankAccountResponse(None))
        }
    }

  def eventHandler(context: ActorContext[Command]): (State, Event) => State = (state, event) =>
    event match {
      case BankAccountCreated(id) =>
        val account = context.child(id)
          .getOrElse(context.spawn(PersistentBankAccount(id), id))
          .asInstanceOf[ActorRef[Command]]

        state.copy(state.accounts + (id -> account))
    }

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    EventSourcedBehavior[Command, Event, State](
      persistenceId = PersistenceId.ofUniqueId("Bank"),
      emptyState = State(Map.empty),
      commandHandler = commandHandler(context),
      eventHandler = eventHandler(context)
    )
  }
}

object BankPlayground {
  import PersistentBankAccount.Command._, PersistentBankAccount.Response._

  def main(array: Array[String]): Unit = {
    val rootBehavior: Behavior[NotUsed] = Behaviors.setup { context =>
      val bank = context.spawn(Bank(), "DemoBank")

      import akka.actor.typed.scaladsl.AskPattern._
      import scala.concurrent.duration._
      import scala.concurrent.ExecutionContext

      implicit val timeOut: Timeout = Timeout(2.seconds)
      implicit val scheduler: Scheduler = context.system.scheduler
      implicit val ec: ExecutionContext = context.executionContext

      bank.ask(replyTo => CreateBankAccount("zaphod", "EUR", 10, replyTo)). flatMap {
        case BankAccountCreatedResponse(id) =>
          context.log.info(s"successfully created bank account $id")
          bank.ask(replyTo => GetBankAccount(id, replyTo))
      }.foreach {
        case GetBankAccountResponse(maybeBankAccount) =>
          context.log.info(s"Account details: $maybeBankAccount")
      }

      Behaviors.empty
    }

    val system = ActorSystem(rootBehavior, "DemoSystem")

  }
}