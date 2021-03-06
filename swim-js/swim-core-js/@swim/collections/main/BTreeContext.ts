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

import {Objects} from "@swim/util";
import {BTreePage} from "./BTreePage";

/** @hidden */
export abstract class BTreeContext<K, V> {
  pageSplitSize: number;

  compare(x: K, y: K): number {
    return Objects.compare(x, y);
  }

  pageShouldSplit(page: BTreePage<K, V, unknown>): boolean {
    return page.arity > this.pageSplitSize;
  }

  pageShouldMerge(page: BTreePage<K, V, unknown>): boolean {
    return page.arity < this.pageSplitSize >>> 1;
  }
}
BTreeContext.prototype.pageSplitSize = 32;
