# パートナー専用 Slack

## 説明

パートナー同士の連絡をするためのslackクローン。このプラットフォームは、パートナー間のコミュニケーションを強化する。
また、コミュニケーションの一貫で、"助かった！"という感謝の気持ちをポイント化して可視化することを手助けする。

## 主な機能

1. **チャンネル機能**: チャンネルを作成・閲覧する機能。特定のユーザーだけがアクセスできるプライベートチャンネルも提供します。
2. **ポイント化機能**: 相手にやってもらったことをポイントとして集計し、昔の自分たちとの競争や可視化を行います。これにより、お互いの貢献を実感することができます。

## 開発のモチベーション

パートナーの生活を向上させ、お互いの協力と感謝の気持ちを深めるためのプラットフォームを提供することを目的としています。ゲーミフィケーションを取り入れ、お互いの感謝できることに意識を向けることが、関係性向上に欠かせないと思います。


## ロードマップ

- **v0.1.0**
  - CI/CDの構築:
    - テスト機能
    - フォーマッター
    - リンター
    - GitHub Actionsの利用
    - dhallを利用
  - クライアントサイド:
    - チャンネルIDによるメッセージのフィルタリング
    - Websocketによるメッセージのリアルタイム受信
    - APIを利用したメッセージ送信（オフライン時は送信しない）
  - バックエンドサイド:
    - clojureでのlambdaの実装
    - Websocket通信の確立

- **v0.2.0**
  - クライアントサイド:
    - オフラインキャッシュ機能
    - 文字以外のアイコン表示
    - 相手の入力状態の表示
  - バックエンドサイド:
    - 通信が切れたときのメッセージキャッシュ機能

- **v0.3.0**
  - モバイル対応:
    - バックグラウンドでの再試行機能
    - オフラインキャッシュの強化
    - 通知機能
## 技術スタック

**Client:** ClojureScript, TailwindCSS

**Server:** Clojure, Lambda

![Logo]()
