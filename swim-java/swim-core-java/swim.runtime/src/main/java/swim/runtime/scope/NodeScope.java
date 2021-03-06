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

package swim.runtime.scope;

import swim.api.downlink.EventDownlink;
import swim.api.downlink.ListDownlink;
import swim.api.downlink.MapDownlink;
import swim.api.downlink.ValueDownlink;
import swim.api.http.HttpDownlink;
import swim.api.ref.LaneRef;
import swim.api.ref.NodeRef;
import swim.concurrent.Stage;
import swim.runtime.CellContext;
import swim.runtime.downlink.EventDownlinkView;
import swim.runtime.downlink.MapDownlinkView;
import swim.runtime.downlink.ValueDownlinkView;
import swim.structure.Form;
import swim.structure.Value;
import swim.uri.Uri;
import swim.warp.CommandMessage;

public class NodeScope extends Scope implements NodeRef {
  protected final Uri meshUri;
  protected final Uri hostUri;
  protected final Uri nodeUri;

  public NodeScope(CellContext cellContext, Stage stage, Uri meshUri,
                   Uri hostUri, Uri nodeUri) {
    super(cellContext, stage);
    this.meshUri = meshUri;
    this.hostUri = hostUri;
    this.nodeUri = nodeUri;
  }

  public final Uri meshUri() {
    return this.meshUri;
  }

  @Override
  public final Uri hostUri() {
    return this.hostUri;
  }

  @Override
  public final Uri nodeUri() {
    return this.nodeUri;
  }

  @Override
  public LaneRef laneRef(Uri laneUri) {
    return new LaneScope(cellContext, stage, this.meshUri, this.hostUri, this.nodeUri, laneUri);
  }

  @Override
  public LaneRef laneRef(String laneUri) {
    return laneRef(Uri.parse(laneUri));
  }

  @Override
  public EventDownlink<Value> downlink() {
    return new EventDownlinkView<Value>(this, stage, this.meshUri, this.hostUri, this.nodeUri,
        Uri.empty(), 0.0f, 0.0f, Value.absent(), Form.forValue());
  }

  @Override
  public ListDownlink<Value> downlinkList() {
    return null; // TODO
  }

  @Override
  public MapDownlink<Value, Value> downlinkMap() {
    return new MapDownlinkView<Value, Value>(this, stage, this.meshUri, this.hostUri,
        this.nodeUri, Uri.empty(), 0.0f, 0.0f, Value.absent(), Form.forValue(), Form.forValue());
  }

  @Override
  public ValueDownlink<Value> downlinkValue() {
    return new ValueDownlinkView<Value>(this, stage, this.meshUri, this.hostUri, this.nodeUri,
        Uri.empty(), 0.0f, 0.0f, Value.absent(), Form.forValue());
  }

  @Override
  public <T> HttpDownlink<T> downlinkHttp() {
    return null; // TODO
  }

  @Override
  public void command(Uri laneUri, float prio, Value body) {
    final CommandMessage message = new CommandMessage(this.nodeUri, laneUri, body);
    pushDown(new ScopePushRequest(this.meshUri, this.hostUri, null, message, prio));
  }

  @Override
  public void command(String laneUri, float prio, Value body) {
    command(Uri.parse(laneUri), prio, body);
  }

  @Override
  public void command(Uri laneUri, Value body) {
    command(laneUri, 0.0f, body);
  }

  @Override
  public void command(String laneUri, Value body) {
    command(Uri.parse(laneUri), body);
  }
}
