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

package swim.runtime.router;

import swim.api.auth.Credentials;
import swim.api.auth.Identity;
import swim.api.data.DataFactory;
import swim.api.downlink.Downlink;
import swim.api.policy.Policy;
import swim.api.policy.PolicyDirective;
import swim.concurrent.Schedule;
import swim.concurrent.Stage;
import swim.math.Z2Form;
import swim.runtime.HostBinding;
import swim.runtime.HostContext;
import swim.runtime.HttpBinding;
import swim.runtime.LaneBinding;
import swim.runtime.LinkBinding;
import swim.runtime.NodeBinding;
import swim.runtime.PushRequest;
import swim.store.ListDataBinding;
import swim.store.MapDataBinding;
import swim.store.SpatialDataBinding;
import swim.store.ValueDataBinding;
import swim.structure.Value;
import swim.uri.Uri;

public class PartTableHost implements HostContext {
  protected final PartTable part;

  protected final HostBinding host;

  protected final Uri hostUri;

  public PartTableHost(PartTable part, HostBinding host, Uri hostUri) {
    this.part = part;
    this.host = host;
    this.hostUri = hostUri;
  }

  @Override
  public final Uri meshUri() {
    return this.part.meshUri();
  }

  @Override
  public final Value partKey() {
    return this.part.partKey();
  }

  @Override
  public final Uri hostUri() {
    return this.hostUri;
  }

  @Override
  public Policy policy() {
    return this.part.policy();
  }

  @Override
  public Schedule schedule() {
    return this.part.schedule();
  }

  @Override
  public Stage stage() {
    return this.part.stage();
  }

  @Override
  public DataFactory data() {
    return this.part.data();
  }

  @Override
  public NodeBinding createNode(Uri nodeUri) {
    return this.part.partContext().createNode(this.hostUri, nodeUri);
  }

  @Override
  public NodeBinding injectNode(Uri nodeUri, NodeBinding node) {
    return this.part.partContext().injectNode(this.hostUri, nodeUri, node);
  }

  @Override
  public LaneBinding injectLane(Uri nodeUri, Uri laneUri, LaneBinding lane) {
    return this.part.partContext().injectLane(this.hostUri, nodeUri, laneUri, lane);
  }

  @Override
  public PolicyDirective<Identity> authenticate(Credentials credentials) {
    return this.part.partContext().authenticate(credentials);
  }

  @Override
  public ListDataBinding openListData(Value name) {
    return this.part.openListData(name);
  }

  @Override
  public ListDataBinding injectListData(ListDataBinding dataBinding) {
    return this.part.injectListData(dataBinding);
  }

  @Override
  public MapDataBinding openMapData(Value name) {
    return this.part.openMapData(name);
  }

  @Override
  public MapDataBinding injectMapData(MapDataBinding dataBinding) {
    return this.part.injectMapData(dataBinding);
  }

  @Override
  public <S> SpatialDataBinding<S> openSpatialData(Value name, Z2Form<S> shapeForm) {
    return this.part.openSpatialData(name, shapeForm);
  }

  @Override
  public <S> SpatialDataBinding<S> injectSpatialData(SpatialDataBinding<S> dataBinding) {
    return this.part.injectSpatialData(dataBinding);
  }

  @Override
  public ValueDataBinding openValueData(Value name) {
    return this.part.openValueData(name);
  }

  @Override
  public ValueDataBinding injectValueData(ValueDataBinding dataBinding) {
    return this.part.injectValueData(dataBinding);
  }

  @Override
  public LinkBinding bindDownlink(Downlink downlink) {
    return this.part.bindDownlink(downlink);
  }

  @Override
  public void openDownlink(LinkBinding link) {
    this.part.openDownlink(link);
  }

  @Override
  public void closeDownlink(LinkBinding link) {
    // nop
  }

  @Override
  public void httpDownlink(HttpBinding http) {
    this.part.httpDownlink(http);
  }

  @Override
  public void pushDown(PushRequest pushRequest) {
    this.part.pushDown(pushRequest);
  }

  @Override
  public void trace(Object message) {
    this.part.trace(message);
  }

  @Override
  public void debug(Object message) {
    this.part.debug(message);
  }

  @Override
  public void info(Object message) {
    this.part.info(message);
  }

  @Override
  public void warn(Object message) {
    this.part.warn(message);
  }

  @Override
  public void error(Object message) {
    this.part.error(message);
  }

  @Override
  public void close() {
    this.part.closeHost(this.hostUri);
  }

  @Override
  public void willOpen() {
    // nop
  }

  @Override
  public void didOpen() {
    // nop
  }

  @Override
  public void willLoad() {
    // nop
  }

  @Override
  public void didLoad() {
    // nop
  }

  @Override
  public void willStart() {
    // nop
  }

  @Override
  public void didStart() {
    // nop
  }

  @Override
  public void didConnect() {
    this.part.hostDidConnect(this.hostUri);
  }

  @Override
  public void didDisconnect() {
    this.part.hostDidDisconnect(this.hostUri);
  }

  @Override
  public void willStop() {
    // nop
  }

  @Override
  public void didStop() {
    // nop
  }

  @Override
  public void willUnload() {
    // nop
  }

  @Override
  public void didUnload() {
    // nop
  }

  @Override
  public void willClose() {
    // nop
  }
}
