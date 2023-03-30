ANSI Formatting
===============

The ``io.aviso.ansi`` namespace defines a number of functions and constants for producing
`ANSI escape codes <https://en.wikipedia.org/wiki/ANSI_escape_code>`_.

Starting with 1.4, the ``compose`` function is the best way to construct text with ANSI escape codes:

.. image:: images/ansi-compose.png
   :alt: Example ANSI formatting


``compose`` uses a `Hiccup <https://github.com/weavejester/hiccup>`_ inspired data structure to identify how different vector blocks of text should be formatted.

Constants and Functions
-----------------------

``compose`` is built on top of a large number of underlying functions and constants.

ANSI supports eight named colors, each with a bright variant.
For each of the supported colors (black, red, green, yellow, blue, magenta, cyan, and white) there will be four functions and four constants:

* [bright-] *color* - function to set foreground text color
* [bright-] *color*-bg - function to set background color
* [bright-] *color*-font - constant that enables the text color
* [bright-] *color*-bg-font - constant that enables the color as background

For example, for the color green there will be ``green``, ``green-bg``, ``bright-green``, and ``bright-green-bg`` functions,
and constants ``green-font``, ``green-bg-font``, ``bright-green-font``, and ``bright-green-bg-font``.

The functions are passed a string and wrap the string with ANSI codes to enable the specific font attributes, with
a reset of all attributes after the string.

Note that the exact color interpretation of the ANSI codes varies significantly between platforms and applications, and
is frequently configurable, often using themes.
You may need to adjust your application's settings to get an optimum display.

In addition to color, text can be:
* ``bold``, ``faint``, or ``plain``
* ``italic`` or ``roman``
* ``inverse`` (which inverts the  foreground and background colors) or ``normal``
* ``underlined`` or ``not-underlined``

For each of these, there is a function and a ``-font`` constant.

Finally, ``reset-font`` is a constant that reverts all font characteristics back to defaults.
