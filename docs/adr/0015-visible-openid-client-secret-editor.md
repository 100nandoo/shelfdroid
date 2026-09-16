# Visible OpenID Client Secret Editor

ShelfDroid will present the OpenID client secret as an ordinary editable text field in the
administrator's **Authentication settings** editor. This records the UI decision implemented by
`59676764`, which replaced the masked password-field presentation with the shared outlined text
field while retaining ordinary edit and IME navigation behavior. The server and data layers will
continue to treat the value as sensitive: redacting it from diagnostic string output and sending
it only when the administrator changes it. This choice is accepted because the Authentication
settings screen is an administrator-only configuration surface and the refactor's intended
editing flow treats the configured value as directly editable. Any later change to masking,
reveal controls, accessibility semantics, or platform autofill behavior requires revisiting this
decision and its UI tests.
