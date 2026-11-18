(ns itonami.app.entry
  "Minori on the device (AWAI-3502). Calls minori.score, minori.react,
  minori.ceiling, minori.reach and minori.social unchanged in behaviour. The
  file-reading parts (snapshot, ledger, roster) stay JVM-only; the model and
  numbers are typed in. The digest is prepared, never sent."
  (:require [itonami.app.run :as run]
            [minori.ceiling :as ceiling]
            [minori.reach :as reach]
            [minori.react :as react]
            [minori.score :as score]
            [minori.social :as social]))

(def ^:private model
  "{:weights {:eta 0.35 :adoption 0.30 :capture 0.20 :phi 0.15}
   :targets {:eta 1.0 :capture 0.01 :phi-potential 9.8 :adoption 100}}")

(run/register!
 {:name "growth" :label "成長 G を計算する"
  :help "η・採用・捕捉・Φ から G と報酬を計算します。η が測定値（:eta-grounded）で 1 に届くまでは、純粋な成長は報酬になりません（非寄生のゲート）。モデルは repo の system.edn と同じです。"
  :example (str "{:state {:eta-estimate 0.82 :capture-estimate 0.001 :phi-realized 3.4}
 :adoption {:p 0.3}
 :model " model "}")
  :run (fn [{:keys [state adoption model]}] (score/growth state model adoption))})

(run/register!
 {:name "beat" :label "次の一手を試算する"
  :help "憲章に沿う介入（観測・計測・共生・下書き）だけを候補に、伸び × 良さ ÷ 費用が最大のものを選び、適用後の G を試算します（dry-run）。"
  :example (str "{:loop {:state {:eta-estimate 0.82 :capture-estimate 0.001 :phi-realized 3.4} :done #{}}
 :adoption {:p 0.3}
 :model " model "}")
  :run (fn [{:keys [loop model adoption]}] (react/beat loop model adoption))})

(run/register!
 {:name "ceiling" :label "伸びしろを評価する"
  :help "自分で動かせる伸びしろ（η）と、外部の条件待ちの伸びしろ（採用・捕捉・Φ）を分け、頭打ちかどうかを判定します。"
  :example "{:weights {:eta 0.35 :adoption 0.30 :capture 0.20 :phi 0.15}
 :components {:eta 0.82 :adoption 0.3 :capture 0.1 :phi 0.35}
 :recent-dG [0.0 0.0]}"
  :run (fn [{:keys [weights components recent-dG]}] (ceiling/evaluate weights components {:recent-dG recent-dG}))})

(run/register!
 {:name "reach" :label "届いた数を見る"
  :help "会員が送った記録の数から、届いた数を上限 3 で数えます。反応の数（いいね等）は扱いません。"
  :example "{:send-receipts [\"receipt-1\" \"receipt-2\"]}"
  :run reach/observe})

(run/register!
 {:name "digest" :label "案内を下書きする"
  :help "観測した数字から、寄付・協力の案内を下書きします。送信はしません（会員が確認して送ります）。急かす表現が入らないことを確かめます。"
  :example "{:eta 0.82 :adopted 18 :realized-phi 2.89 :next-step \"deepen-symbiosis\" :next-gate :earned}"
  :run (fn [m] (let [d (social/digest m)] d))})
