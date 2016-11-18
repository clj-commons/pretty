REPL
====

Pretty includes some useful REPL utilities in the ``io.aviso.repl`` namespace.

Primarily, the ``install-pretty-exceptions`` function overrides several
internal Clojure functions to enable
:doc:`formatted exceptions <exceptions>`.
This function is normally invoked for you
when Pretty is
:doc:`used as a Leiningen plugin <lein-plugin>`.

The remaining functions exist to help you make things pretty
that *don't* originate in the REPL. You will often see output in logs: EDN data perhaps,
or often, raw exceptions.

If you have a REPL running, you can use the following functions to get a better view
of that data:

copy
----

The ``copy`` function will return the current contents of the system clipboard
as a string.
This requires that AWT is running.
On OS X, you will see a window for your application start when you first invoke this function

The ``pretty-print`` and ``format-exception`` functions can be invoked
with no arguments, in which case the call to ``copy`` happens automatically.

Consult the API documentation for more details.

pretty-print
------------

This will pretty-print the contents of the clipboard; the clipboard text is parsed as EDN.

format-exception
----------------

This will parse a normal Java stack trace and format it for readability.

paste
-----

This will copy a string back on to the clipboard, so it can be pasted into
another window.

.. code-block:: clojure

    (use 'io.aviso.repl)

    (-> (copy) format-exception paste)

