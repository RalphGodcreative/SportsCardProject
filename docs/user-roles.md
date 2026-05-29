# User Roles

## Roles

### ADMIN
- Full access to all features
- Add, edit, delete cards
- Manage transactions
- Manage crawler keywords
- Trigger crawler (local dev only — cron job in production)
- Access all REST endpoints

### USER
- Read-only access
- View cards and card details
- View transactions
- Cannot add, edit, or delete anything

## Future Roles (Planned)
- `MODERATOR` — can manage content but not system settings (TBD)

## Public Launch Considerations (Post "Put It Online")
- Add a `username` field to the `User` entity — used as the public-facing display name in the UI
- Email stays as the private login identifier and is never displayed publicly
- Username must be unique, chosen at registration
- Any card listings, transaction views, or shared pages should show `username`, not email
