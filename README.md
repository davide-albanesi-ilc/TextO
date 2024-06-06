# TextO

TextO is a library for corpus management and text annotation. It is currently used as one of the backend module of Maia, a web and collaborative tool for text annotation, lexicon editing and lexical linking being developed at CNR-ILC.

TextO allows to upload both raw plain-text documents (UTF-8 encoded), and also documents having an internal, hierarchically organized, logical structure. This can be done by providing the text with a separator at the beginning of each section. The separator must respect the following pattern:

`#+\(type=".*", title=".*", index=".+"\)`

In other words, the separator must start with one or more “#” characters, to indicate the nesting level, followed by the data describing the section with three attributes: a type (e.g. “chapter”),  a title (e.g. “Chapter 1”), and an index, used internally to denote the section. The three attributes can also appear in different order.

Here is an example of text (extracted from the Decameron by Giovanni Boccaccio) marked with a three-level  structure:

`#(type="Giornata", title="Giornata I", index="I")`  
`I`  
`##(type="Novella", title="Novella 1", index="I.1")`  
`I.1`  
`###(type="Paragrafo", title="Paragrafo 1", index="I.1.1")`  
`Ser Cepparello con una falsa confessione inganna un santo frate e muorsi [...]`  
`###(type="Paragrafo", title="Paragrafo 2", index="I.1.2")`  
`Convenevole cosa è, carissime donne, che ciascheduna cosa la quale l’uomo fa [...]`  

## Installation instructions
