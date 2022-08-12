package com.zaphod.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import scala.util.{Failure, Success, Try}

object PersistentBankAccount {

  sealed trait Command
  object Command {
    case class CreateBankAccount(user: String, currency: String, initialBalance: Double, replyTo: ActorRef[Response]) extends Command
    case class UpdateBalance(id: String, currency: String, amount: Double, replyTo: ActorRef[Response]) extends Command
    case class GetBankAccount(id: String, replyTo: ActorRef[Response]) extends Command
  }

  trait Event
  case class BankAccountCreated(bankAccount: BankAccount) extends Event
  case class BalanceUpdated(amount: Double) extends Event

  sealed trait Response
  object Response {
    case class BankAccountCreatedResponse(id: String) extends Response
    case class BankAccountBalanceUpdatedResponse(maybeBankAccount: Try[BankAccount]) extends Response
    case class GetBankAccountResponse(maybeBankAccount: Option[BankAccount]) extends Response
  }

  // state
  case class BankAccount(id: String, user: String, currency: String, balance: Double)

  import Command._, Response._

  // command handler = message handler => persist an event
  // event handler => update state
  // state

  val commandHandler: (BankAccount, Command) => Effect[Event, BankAccount] = (state, command) =>
    command match {
      case CreateBankAccount(user, currency, initialBalance, replyTo) =>
        val id = state.id

        Effect
        .persist(BankAccountCreated(BankAccount(id, user, currency, initialBalance)))
        .thenReply(replyTo)(_ => BankAccountCreatedResponse(id))

      case UpdateBalance(_, _, amount, replyTo) =>
        val newBalance = state.balance + amount

        if (newBalance < 0)
          Effect.reply(replyTo)(BankAccountBalanceUpdatedResponse(Failure(new RuntimeException("Cannot withdraw more than available"))))
        else
          Effect
          .persist(BalanceUpdated(amount))
          .thenReply(replyTo)(newState => BankAccountBalanceUpdatedResponse(Success(newState)))

      case GetBankAccount(_, replyTo) =>
        Effect.reply(replyTo)(GetBankAccountResponse(Some(state)))
    }

  val eventHandler: (BankAccount, Event) => BankAccount = (state, event) =>
    event match {
      case BankAccountCreated(bankAccount) => bankAccount
      case BalanceUpdated(amount) => state.copy(balance = state.balance + amount)
    }

  def apply(id: String): Behavior[Command] =
    EventSourcedBehavior[Command, Event, BankAccount](
      persistenceId = PersistenceId.ofUniqueId(id),
      emptyState = BankAccount(id, "", "", 0.0),
      commandHandler = commandHandler,
      eventHandler = eventHandler
    )
}
