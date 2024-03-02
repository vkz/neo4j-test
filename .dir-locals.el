;;; Directory Local Variables            -*- no-byte-compile: t -*-
;;; For more information see (info "(emacs) Directory Variables")
;;; Per-directory local variables modified from version in guix checkout

((python-ts-mode . ((eval . (electric-pair-local-mode 1))
                    (auto-revert-interval . 2)))

 ;; C-u M-x cider-jack-in will let you override these nicely e.g. change :main:dev to :main:test
 (clojure-mode  (cider-clojure-cli-aliases . ":main:dev")
                (fill-column . 100)))
