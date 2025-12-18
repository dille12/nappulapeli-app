# Shop update packet examples

The client expects a flat `shopUpdate` payload with a `nextWeapon` object and an
`items` array. Wrapping `items` inside another object (e.g. `{ "items": [...] }`)
won't be parsed, because the app reads the array directly from the top-level
`items` field in `MainActivity.handleMessage`.

## Minimal example
```json
{
  "type": "shopUpdate",
  "nextWeapon": {
    "name": "Rifle",
    "price": 900,
    "image": "<base64 or empty string>",
    "backgroundColor": [80, 120, 255]
  },
  "items": [
    {
      "name": "Grenade",
      "price": 300,
      "image": "<base64 or empty string>",
      "description": "Frag grenades for extra explosive damage.",
      "backgroundColor": [100, 100, 255]
    },
    {
      "name": "Item 2",
      "price": 300,
      "image": null,
      "description": "String (optional)",
      "backgroundColor": [255, 100, 100]
    }
  ]
}
```

### Notes
- `items` must be an array at the top level. If you wrap it in another object
  (`{"items": {"items": [...]}}`), the client will think there are zero items
  and show the "No items available" message.
- `image` may be an empty string or `null`.
- `backgroundColor` accepts CSS-style strings, `{r,g,b}` objects, or `[r,g,b]`
  arrays; the array form above is valid.
```
