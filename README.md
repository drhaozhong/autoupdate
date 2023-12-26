Autoupdate: A tool for upgrading Java client code
========================================


Autoupdate is a tool for upgrading Java client code. After a library of a client project is upgraded, the breaking changes will lead to many compilation errors in this project. This tool modifies source files according to the types of compilation errors until the predefined threshold is met. 

Autoupdate is built upon Astor[https://github.com/SpoonLabs/astor]. It hacks Astor to guide the modifications with compilation errors. Autoupdate is built upon an old version of Astor, since it was implemented in 2019. As a result, it uses J2SE 1.8 and it can have bugs that are already fixed in the latest Astor. In addition, the hacking makes it somewhat difficult to understand. Still, it illustrates the possibility of migrating under the guidance of compilation errors. 


