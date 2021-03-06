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

package swim.runtime.downlink;

import java.util.concurrent.ConcurrentLinkedQueue;
import swim.structure.Value;
import swim.uri.Uri;

public abstract class SupplyDownlinkModem<View extends DownlinkView> extends DownlinkModel<View> {
  final ConcurrentLinkedQueue<Value> upQueue;

  public SupplyDownlinkModem(Uri meshUri, Uri hostUri, Uri nodeUri, Uri laneUri,
                             float prio, float rate, Value body) {
    super(meshUri, hostUri, nodeUri, laneUri, prio, rate, body);
    this.upQueue = new ConcurrentLinkedQueue<Value>();
  }

  @Override
  protected boolean upQueueIsEmpty() {
    return this.upQueue.isEmpty();
  }

  @Override
  protected void queueUp(Value body) {
    this.upQueue.add(body);
  }

  @Override
  protected Value nextUpQueue() {
    return this.upQueue.poll();
  }
}
