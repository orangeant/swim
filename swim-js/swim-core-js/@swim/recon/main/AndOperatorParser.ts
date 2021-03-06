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

import {Builder} from "@swim/util";
import {Input, Parser} from "@swim/codec";
import {ReconParser} from "./ReconParser";

/** @hidden */
export class AndOperatorParser<I, V> extends Parser<V> {
  private readonly _recon: ReconParser<I, V>;
  private readonly _builder: Builder<I, V> | undefined;
  private readonly _lhsParser: Parser<V> | undefined;
  private readonly _rhsParser: Parser<V> | undefined;
  private readonly _step: number | undefined;

  constructor(recon: ReconParser<I, V>, builder?: Builder<I, V>,
              lhsParser?: Parser<V>, rhsParser?: Parser<V>, step?: number) {
    super();
    this._recon = recon;
    this._builder = builder;
    this._lhsParser = lhsParser;
    this._rhsParser = rhsParser;
    this._step = step;
  }

  feed(input: Input): Parser<V> {
    return AndOperatorParser.parse(input, this._recon, this._builder, this._lhsParser,
                                   this._rhsParser, this._step);
  }

  static parse<I, V>(input: Input, recon: ReconParser<I, V>, builder?: Builder<I, V>,
                     lhsParser?: Parser<V>, rhsParser?: Parser<V>, step: number = 1): Parser<V> {
    let c = 0;
    do {
      if (step === 1) {
        if (!lhsParser) {
          lhsParser = recon.parseBitwiseOrOperator(input, builder);
        }
        while (lhsParser.isCont() && !input.isEmpty()) {
          lhsParser = lhsParser.feed(input);
        }
        if (lhsParser.isDone()) {
          step = 2;
        } else if (lhsParser.isError()) {
          return lhsParser.asError();
        }
      }
      if (step === 2) {
        if (input.isCont()) {
          c = input.head();
          if (c === 38/*'&'*/) {
            // first '&' consumed by BitwiseAndOperatorParser
            input = input.step();
            step = 3;
          } else {
            return lhsParser!;
          }
        } else if (input.isDone()) {
          return lhsParser!;
        }
      }
      if (step === 3) {
        if (!rhsParser) {
          rhsParser = recon.parseBitwiseOrOperator(input, builder);
        }
        while (rhsParser.isCont() && !input.isEmpty()) {
          rhsParser = rhsParser.feed(input);
        }
        if (rhsParser.isDone()) {
          const lhs = lhsParser!.bind();
          const rhs = rhsParser.bind();
          lhsParser = Parser.done(recon.and(lhs, rhs));
          rhsParser = void 0;
          step = 2;
          continue;
        } else if (rhsParser.isError()) {
          return rhsParser.asError();
        }
      }
      break;
    } while (true);
    return new AndOperatorParser<I, V>(recon, builder, lhsParser, rhsParser, step);
  }
}
