package com.zaphod.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

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
