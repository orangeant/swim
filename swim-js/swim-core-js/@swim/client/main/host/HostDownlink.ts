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

import {Uri} from "@swim/uri";
import {EventMessage, LinkedResponse, SyncedResponse, UnlinkRequest, UnlinkedResponse} from "@swim/warp";
import {Host} from "./Host";

/** @hidden */
export interface HostDownlink {
  nodeUri(): Uri;
 
  laneUri(): Uri;
 
  onEventMessage(message: EventMessage, hsot: Host): void;
 
  onLinkedResponse(response: LinkedResponse, hsot: Host): void;
 
  onSyncedResponse(response: SyncedResponse, hsot: Host): void;
 
  onUnlinkRequest(request: UnlinkRequest, host: Host): void;
 
  onUnlinkedResponse(response: UnlinkedResponse, hsot: Host): void;
 
  hostDidConnect(host: Host): void;
 
  hostDidDisconnect(host: Host): void;
 
  hostDidFail(error: unknown, host: Host): void;
 
  openUp(host: Host): void;
 
  closeUp(host: Host): void;
}
