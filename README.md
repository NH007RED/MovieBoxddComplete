# MovieBox v18.0 — Movie & Web Series Streaming Catalog with 5-Digit Share Code Redeem, VIP Points & Admin Panel

## What's new in v18.0
- **Points Added Strictly on 5-Digit Share Code Redemption**:
  - Removed the "Refer to Get VIP" share button that previously added points on button press.
  - Points and Share Count are now strictly awarded **only when a 5-digit Share Code is entered and redeemed** (`UserPresenceManager.redeemShareCode`), crediting both the redeemer and the code owner in Firebase Realtime Database (`/users/{uid}` and `/shareCodes/{shareCode}`).
- **Polished Mobile Layouts Across Admin Panel & User Panel**:
  - **Admin Panel (`AdminUsersScreen.kt`)**: Redesigned as a single smooth `LazyColumn` with a balanced **2×2 KPI Grid** (`Total Users`, `Online Now`, `Offline Users`, `VIP Unlocked`), a clean **2-column VIP Points configuration card** (`VIP Points to Access` and `Pts / Code Redeem`), and clean user cards that open the **User Full Details** dialog on click.
  - **User Profile Page (`UserSettingsScreen.kt`)**: Removed the "Refer to Get VIP" button, "Clear Cache" row, and "Open Admin Panel" section; redesigned with a balanced 2-column **My Points / Share Count** layout, a copyable **5-Digit Share Code & Real Device ID** card, and a full-width **Redeem 5-Digit Share Code** input and button.
  - **VIP Access Required / Share Code Screen (`DetailsScreen.kt`)**: Converted from a floating dialog to a native full-screen page inside `DetailsScreen` with proper status-bar and keyboard (`imePadding()`) insets, balanced 2-column stats, copyable 5-digit Share Code, and full-width 5-digit code redemption.
