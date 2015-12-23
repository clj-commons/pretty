.. io.aviso/pretty documentation master file, created by
   sphinx-quickstart on Fri Dec  4 13:43:30 2015.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

io.aviso/pretty
===============

.. image:: http://clojars.org/io.aviso/pretty/latest-version.svg
   :alt: Clojars Project
   :target: http://clojars.org/io.aviso/pretty

Sometimes, neatness counts
--------------------------

If you are trying to puzzle out a stack trace,
pick a critical line of text out of a long stream of console output,
or compare two streams of binary data, a little bit of formatting can go a long way.

That's what the **io.aviso/pretty** library is for.
It adds support for pretty output where it counts:

* Readable output for exceptions
* ANSI font and background color support
* Hex dump of binary data
* Hex dump of binary deltas
* Formatting data into columns

Here's an example of pretty at work:

.. image:: images/formatted-exception.png
   :alt: Formatted Exception

License
-------

Pretty is released under the terms of the `Apache Software License 2.0 <http://www.apache.org/licenses/LICENSE-2.0>`_.

.. toctree::
   :hidden:

   ansi
   exceptions
   lein-plugin
   binary
   columns


   API <http://avisonovate.github.io/docs/pretty>
   GitHub Project <https://github.com/AvisoNovate/pretty>

