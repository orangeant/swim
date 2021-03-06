// Copyright 2015-2019 SWIM.AI inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

import {TestOptions, Test, Spec, Report} from "@swim/unit";
import {Attr, Slot, Value, Record, Text} from "@swim/structure";
import {
  Envelope,
  EventMessage,
  CommandMessage,
  LinkRequest,
  LinkedResponse,
  AuthRequest,
  AuthedResponse,
  DeauthedResponse,
} from "@swim/warp";
import {Uri} from "@swim/uri";
import {Host, SwimClient} from "@swim/client";
import {MockServer} from "./MockServer";
import {ClientExam} from "./ClientExam";

export class SwimClientSpec extends Spec {
  createExam(report: Report, name: string, options: TestOptions): ClientExam {
    return new ClientExam(report, this, name, options);
  }

  @Test
  clientDidConnect(exam: ClientExam): Promise<void> {
    return exam.mockServer((server: MockServer, client: SwimClient, resolve: () => void): void => {
      client.didConnect(function (host: Host): void {
        exam.comment("didConnect");
        exam.equal(host.hostUri(), server.hostUri());
        resolve();
      });
      client.command(server.hostUri(), "/", "connect");
    });
  }

  @Test
  clientDidDisconnect(exam: ClientExam): Promise<void> {
    return exam.mockServer((server: MockServer, client: SwimClient, resolve: () => void): void => {
      client.didConnect(function (host: Host): void {
        exam.comment("didConnect");
        client.close();
      });
      client.didDisconnect(function (host: Host): void {
        exam.comment("didDisconnect");
        exam.equal(host.hostUri(), server.hostUri());
        resolve();
      });
      client.command(server.hostUri(), "/", "connect");
    });
  }

  @Test
  clientDidAuthenticate(exam: ClientExam): Promise<void> {
    return exam.mockServer((server: MockServer, client: SwimClient, resolve: () => void): void => {
      server.onEnvelope = function (envelope: Envelope): void {
        exam.true(envelope instanceof AuthRequest);
        exam.equal(envelope.body(), Record.of(Slot.of("key", 1234)));
        server.send(AuthedResponse.of(Record.of(Slot.of("id", 5678))));
      };
      client.didAuthenticate(function (body: Value, host: Host): void {
        exam.comment("didAuthenticate");
        exam.equal(host.hostUri(), server.hostUri());
        exam.equal(body, Record.of(Slot.of("id", 5678)));
        resolve();
      });
      client.authenticate(server.hostUri(), Record.of(Slot.of("key", 1234)));
    });
  }

  @Test
  clientDidDeauthenticate(exam: ClientExam): Promise<void> {
    return exam.mockServer((server: MockServer, client: SwimClient, resolve: () => void): void => {
      server.onEnvelope = function (envelope: Envelope): void {
        exam.true(envelope instanceof AuthRequest);
        exam.equal(envelope.body(), Record.of(Slot.of("key", 1234)));
        server.send(DeauthedResponse.of(Record.of(Attr.of("denied"))));
      };
      client.didAuthenticate(function (body: Value, host: Host): void {
        exam.fail("didAuthenticate");
      });
      client.didDeauthenticate(function (body: Value, host: Host): void {
        exam.comment("didDeauthenticate");
        exam.equal(host.hostUri(), server.hostUri());
        exam.equal(body, Record.of(Attr.of("denied")));
        resolve();
      });
      client.authenticate(server.hostUri(), Record.of(Slot.of("key", 1234)));
    });
  }

  @Test
  clientDownlink(exam: ClientExam): Promise<void> {
    return exam.mockServer((server: MockServer, client: SwimClient, resolve: () => void): void => {
      server.onEnvelope = function (envelope: Envelope): void {
        exam.true(envelope instanceof LinkRequest);
        exam.equal(envelope.node(), Uri.parse("house/kitchen"));
        exam.equal(envelope.lane(), Uri.parse("light"));
        server.send(LinkedResponse.of(envelope.node(), envelope.lane()));
        server.send(EventMessage.of(envelope.node(), envelope.lane(), "on"));
      };
      client.downlink()
        .hostUri(server.hostUri())
        .nodeUri("house/kitchen")
        .laneUri("light")
        .keepLinked(false)
        .willLink(function (): void {
          exam.comment("willLink");
        })
        .didLink(function (): void {
          exam.comment("didLink");
        })
        .onEvent(function (body: Value): void {
          exam.comment("onEvent");
          exam.equal(body, Text.from("on"));
          resolve();
        })
        .open();
    });
  }

  @Test
  clientCommand(exam: ClientExam): Promise<void> {
    return exam.mockServer((server: MockServer, client: SwimClient, resolve: () => void): void => {
      server.onEnvelope = function (envelope: Envelope): void {
        exam.true(envelope instanceof CommandMessage);
        exam.equal(envelope.node(), Uri.parse("house/kitchen"));
        exam.equal(envelope.lane(), Uri.parse("light"));
        exam.equal(envelope.body(), Text.from("on"));
        resolve();
      };
      client.command(server.hostUri(), "house/kitchen", "light", "on");
    });
  }
}
