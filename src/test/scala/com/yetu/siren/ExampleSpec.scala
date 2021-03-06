/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 yetu AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.yetu.siren

import org.scalatest.{ MustMatchers, WordSpec }
import spray.json._
import json.sprayjson.SirenJsonProtocol
import model._
import Entity.{ EmbeddedRepresentation, EmbeddedLink, RootEntity }

class ExampleSpec extends WordSpec with MustMatchers {

  case class Item(productCode: String, quantity: Int)

  case class Customer(customerId: String, name: String)

  case class Order(orderNumber: Int, itemCount: Int, status: String, items: Seq[Item], customer: Customer)

  implicit def orderSirenWriter(implicit baseUri: String): SirenRootEntityWriter[Order] =
    new SirenRootEntityWriter[Order] {
      override def toSiren(order: Order) = {
        RootEntity(
          classes = Some(List("order")),
          properties = Some(List(
            Property("orderNumber", Property.NumberValue(order.orderNumber)),
            Property("itemCount", Property.NumberValue(order.itemCount)),
            Property("status", Property.StringValue(order.status))
          )),
          entities = Some(List(
            EmbeddedLink(
              classes = Some(List("items", "collection")),
              rel = "http://x.io/rels/order-items" :: Nil,
              href = s"$baseUri/orders/42/items"
            ),
            EmbeddedRepresentation(
              classes = Some(List("info", "customer")),
              rel = "http://x.io/rels/customer" :: Nil,
              properties = Some(List(
                Property("customerId", Property.StringValue(order.customer.customerId)),
                Property("name", Property.StringValue(order.customer.name))
              )),
              links = Some(List(Link(
                rel = "self" :: Nil,
                href = s"$baseUri/customers/${order.customer.customerId}"
              )))
            )
          )),
          actions = Some(List(Action(
            name = "add-item",
            title = Some("Add Item"),
            method = Some(Action.Method.POST),
            href = s"$baseUri/orders/${order.orderNumber}/items",
            `type` = Some(Action.Encoding.`application/x-www-form-urlencoded`),
            fields = Some(List(
              Action.Field(
                name = "orderNumber",
                `type` = Action.Field.Type.`hidden`,
                value = Some(order.orderNumber.toString)
              ),
              Action.Field(
                name = "productCode",
                `type` = Action.Field.Type.`text`
              ),
              Action.Field(
                name = "quantity",
                `type` = Action.Field.Type.`number`
              )
            ))
          ))),
          links = Some(List(
            Link(
              rel = "self" :: Nil,
              href = s"$baseUri/orders/${order.orderNumber}"
            ),
            Link(
              rel = "previous" :: Nil,
              href = s"$baseUri/orders/${order.orderNumber - 1}"
            ),
            Link(
              rel = "next" :: Nil,
              href = s"$baseUri/orders/${order.orderNumber + 1}"
            )
          ))
        )
      }
    }

  "Instance of types that belong to the SirenRootEntityWriter type class" must {
    "produce the specified Siren JSON representation" in new SirenJsonProtocol {
      implicit val baseUri: String = "http://api.x.io"
      val order = Order(42, 3, "pending", Seq(Item("ABC", 1)), Customer("pj123", "Peter Joseph"))
      val expectedJson =
        """{
          |  "class": [ "order" ],
          |  "properties": {
          |      "orderNumber": 42,
          |      "itemCount": 3,
          |      "status": "pending"
          |  },
          |  "entities": [
          |    {
          |      "class": [ "items", "collection" ],
          |      "rel": [ "http://x.io/rels/order-items" ],
          |      "href": "http://api.x.io/orders/42/items"
          |    },
          |    {
          |      "class": [ "info", "customer" ],
          |      "rel": [ "http://x.io/rels/customer" ],
          |      "properties": {
          |        "customerId": "pj123",
          |        "name": "Peter Joseph"
          |      },
          |      "links": [
          |        { "rel": [ "self" ], "href": "http://api.x.io/customers/pj123" }
          |      ]
          |    }
          |  ],
          |  "actions": [
          |    {
          |      "name": "add-item",
          |      "title": "Add Item",
          |      "method": "POST",
          |      "href": "http://api.x.io/orders/42/items",
          |      "type": "application/x-www-form-urlencoded",
          |      "fields": [
          |        { "name": "orderNumber", "type": "hidden", "value": "42" },
          |        { "name": "productCode", "type": "text" },
          |        { "name": "quantity", "type": "number" }
          |      ]
          |    }
          |  ],
          |  "links": [
          |    { "rel": [ "self" ], "href": "http://api.x.io/orders/42" },
          |    { "rel": [ "previous" ], "href": "http://api.x.io/orders/41" },
          |    { "rel": [ "next" ], "href": "http://api.x.io/orders/43" }
          |  ]
          |}""".stripMargin.parseJson
      order.rootEntity.toJson mustEqual expectedJson
    }

  }

}
