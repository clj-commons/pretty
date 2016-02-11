Leiningen Plugin
================

pretty can act as a plugin to
`Leiningen <https://github.com/technomancy/leiningen>`_.

To enable pretty exception reporting automatically, add
pretty to *both* the :plugins and the :dependencies lists
of your :file:`project.clj`.

.. code-block:: clojure

  (defproject ...
   :plugins [[io.aviso/pretty "0.1.23"]]
   :dependencies [...
                  [io.aviso/pretty "0.1.23"]]
   ...)


Adjust if necessary, for the current version, "|release|".

This adds middleware to enable pretty exception reporting when running a REPL, tests,
or anything else that starts code in the project.

Another option is to add the following to your :file:`~/.lein/profiles.clj`:

.. code-block:: clojure

   :pretty {
     :plugins [[io.aviso/pretty "0.1.23"]]
     :dependencies [[io.aviso/pretty "0.1.23"]]
   }

This creates an opt-in profile that adds and enables pretty exception reporting.

You can then enable pretty in any project, even one that does not normally have pretty
as a dependency, as follows:

::

   lein with-profiles +pretty run

or

::

   lein with-profiles +pretty do clean, test, install



