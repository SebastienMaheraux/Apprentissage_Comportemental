# Apprentissage_Comportemental

Projet M1 mise en place de process pour la création de bases de données et leur étude

Explication du projet :

1/Creation d'une base de données

2/Exploitation des données

3/Apprentissage et prédictions avec Spark

Les outils :

Scrapy : [Website](http://scrapy.org/)
Spark : [Website](http://spark.apache.org/)
Java : [Website](http://www.java.com)
Scala : [Website](http://www.scala-lang.org/)
Netbeans ou Eclipse

Installation des outils :

SCRAPY : [Website](http://doc.scrapy.org/en/latest/intro/install.html) (dépendances : python, pip)

Sur Ubuntu 9.10 ou supérieur :
```
$ sudo apt-get install python-dev python-pip libxml2-dev libxslt1-dev zlib1g-dev libffi-dev libssl-dev
$ pip install Scrapy
```

JAVA :
```
$ sudo apt-add-repository ppa:webupd8team/java
$ sudo apt-get update
$ sudo apt-get install oracle-java7-installer
$ java -version
```
> java version "1.7.0_80"
> Java(TM) SE Runtime Environment (build 1.7.0_80-b15)
> Java HotSpot(TM) 64-Bit Server VM (build 24.80-b11, mixed mode)

SCALA :
```
$ wget http://www.scala-lang.org/files/archive/scala-2.11.7.deb
$ sudo dpkg -i scala-2.11.7.deb
$ sudo apt-get update
$ sudo apt-get install scala
```

SPARK :
```
$ wget http://d3kbcqa49mib13.cloudfront.net/spark-1.2.0-bin-hadoop2.4.tgz
$ tar -xzvf spark-1.2.0-bin-hadoop2.4.tgz
$ cd spark-1.2.0-bin-hadoop2.4/
$ ./bin/spark-shell
```

Exécution des scripts :

////////////////////////////////////////////////
utiliser le script Scrapy

Ouvrir un terminal depuis le dossier doctissimo (contenant scrapy.cfg), puis entrer la commande 
```
$ rm ./[filename].xml; scrapy crawl doctissimo -o [filename].xml
```

NB : la commande suivante supprime le fichier XML existant, sans quoi des problèmes pourraient survenir lors de l’écriture du fichier :
```
$ rm ./[filename].xml
```

////////////////////////////////////////////////
Convertir le XML
  Script sed
  
  Ouvrir un terminal depuis le dossier contenant formatXML_v2.0.sh, puis entrez la commande :
  ```
  $chmod +x formatXML_v2.0.sh
  $./formatXML_v2.0.sh
  ```
  Ce script génère un fichier xml test.xml, vous en aurez besoin pour l’étape suivante.

  Script JAVA
  
  Copier/coller le document Add_id dans le dossier regroupant vos précédents projets sous Netbeans ou Eclipse.
  Ensuite récupérez le fichier xml généré précédemment par le script sed, ex : test.xml, copier/coller à la racine du projet Add_id.
  
  Sous Netbeans :
  Cliquez sur “Ouvrir un projet”, sélectionnez Add_id
  
  Nous utilisont la bibliothèque JDOM pour l’édition du fichier XML, il va falloir créer cette librairie :
  Clique droit sur le noeud “Libraries” sous votre projet, sélectionnez Add Library…
  Cliquez sur Create, entrer le nom pour identifier votre librairie puis “OK”
  Cliquez sur “Add JAR/Folder…”
  Dans la fenêtre qui s’ouvre retrouvez le dossier Add_id
  Puis dans dist/lib sélectionnez jdom-2.0.6.jar, puis “add”
  
  Une fois la bibliothèque le script devrait s’exécuter sans erreur, un fichier xml data.xml est généré à la racine du projet Add_id, vous aurez besoin de ce fichier pour l’étape suivante.

////////////////////////////////////////////////
analyser la BDD sous Spark

