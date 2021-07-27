# Auto pair

When enabled, typing a parenthesis, bracket or quote will automatically insert the
corresponding closing character too: `'…'`, `"…"`, `(…)`, `[…]`, `{…}`.


# Commands

## Find

To run the **find** command, type `/text to search` (replacing _text to search_
with whatever you want to find). The search is performed from the current cursor
position. It is also possible to search by regular expressions.


## Delete

### All occurrences

To delete all occurrences of a certain string, type `d/text to delete` (replacing
_text to delete_ with whatever you want to delete).

>"A quick fox jumps over the lazy dog. The quick fox is brown"
Running `d/quick`, assuming the cursor is at the beginning of the line, transforms the text to

>"A fox jumps over the lazy dog. The fox is brown"

### First occurrences

To delete a number of occurrences of a certain string (from the current cursor position),
type `N d/text to delete` (replacing _N_ with the number of occurrences to be deleted and
_text to delete_ with whatever you want to delete).

>"A quick fox jumps over the lazy dog. The fox is quick"
Run `1 d/quick` assuming the cursor is at the beginning of the line, transforms the text to

>"A fox jumps over the lazy dog. The fox is quick"

## Substitute

### All occurrences

To delete all occurrences of a certain string, type `d/text to delete` (replacing
_text to delete_ with whatever you want to delete).

>"A quick fox jumps over the lazy dog. The fox is brown"
Running `s/fox/cat`, assuming the cursor is at the beginning of the line, transforms the text to

>"A quick cat jumps over the lazy dog. The cat is brown"

### First occurrences

To substitute a number of occurrences of a certain string with another one (from the
current cursor position), type `N s/text to delete/replacement text` (replacing _N_ with the
number of occurrences to be substituted and _text to delete_ with whatever you want to find
and _replacement text_ with whatever you want to replace the deleted text with).

>"A quick fox jumps over the lazy dog. The fox is quick"
Run `1 s/quick/hungry` assuming the cursor is at the beginning of the line, transforms the text to

>"A hungry fox jumps over the lazy dog. The fox is quick"

## Configuration

Configure the application options with the `set` command:

- `commands`: Change commands field visibility
  - [`on` | `off`]
- `pair`: Enable or disable auto–close brackets and quotes
  - [`on` | `off`]
- `size`: Change text size
  - [`large` | `medium` | `small`]
- `style`: Change text style
  - [`mono` | `sans` | `serif`]

To configure an option with the set command type: `set/name/value` (replacing _name_ with
the name of the option to be configured and _value_ with the desired configuration value).


# Keyboard shortcuts

You can use these handy keyboard shortcuts to help you navigate through the app more
quickly:

- `ctrl` `N`: Create a new file
- `ctrl` `O`: Open a file
- `ctrl` `Q`: Quit
- `ctrl` `S`: Save
- `ctrl` `Z`: Undo
- `ctrl` `+`: Increase text size
- `ctrl` `-`: Decrease text size
- `ctrl` `/`: Show (or hide) command field
