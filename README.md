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
$ wget http://apache.crihan.fr/dist/spark/spark-1.6.0/spark-1.6.0-bin-hadoop2.6.tgz
$ tar -xzvf spark-1.6.0-bin-hadoop2.6.tgz
$ cd spark-1.6.0-bin-hadoop2.6/
$ ./bin/spark-shell
```

Exécution des scripts :


**utiliser le script Scrapy**

Ouvrir un terminal depuis le dossier doctissimo (contenant scrapy.cfg), puis entrer la commande 
```
$ rm ./[filename].xml; scrapy crawl doctissimo -o [filename].xml
```

NB : la commande suivante supprime le fichier XML existant, sans quoi des problèmes pourraient survenir lors de l’écriture du fichier :
```
$ rm ./[filename].xml
```


**Convertir le XML**

 **_Script sed_**
  
  Ouvrir un terminal depuis le dossier contenant formatXML_v2.0.sh, puis entrez la commande :
  ```
  $ chmod +x formatXML_v2.0.sh
  $ ./formatXML_v2.0.sh [input_filename]
  ```
  ex :
  ```
  $ ./formatXML_v2.0.sh scrapedData.xml
  ```
  
  Ce script génère un fichier xml output.xml, vous en aurez besoin pour l’étape suivante.

  **_Script JAVA_**
  
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

**Analyser la BDD sous Spark**


  Prérequis : Spark version 1.6.0
  
  Afin de traiter la BDD avec Spark, il faut que le script .scala et la fichier contenant votre BDD .xml soient dans le même dossier.
  
  Lancez ensuite le script de cette manière :
  
  
  ```
  $ spark-shell -i <file adress>/[filename].scala
  ```

  Pour lancer une nouvelle fois le script .scala sans avoir à fermer puis redémarrer Spark, entrez la commande suivante :
  
  ```
  scala>  :load <file adress>/[filename].scala
  ```
  
  Ce script vous donnera suite à son excéution la probabilité que le message donné dans le code correspond bien au tag également indiqué dans le code. Deux probabilités seront présentées :
    - le taux de vrai-positif, en quelque sorte la vraisemblance du résultat obtenu
    - la probabilité que le message concerne le sujet correspondant au tag indiqué, à prendre avec du recul selon la vraisemblance obtenue précedemment
  
  Un deuxième message est analysé par la même, qui lui concerne une question de programmation : il est ici afin de s'assurer qu'un message n'ayant rien avoir avec le sujet ciblé (l'alimentation dans notre exemple) obtienne bien les probabilités attendues : ~100% de vrai-positif, ~0% de chances qu'il parle d'alimentation.
  
  **Un second script est donné, qui est muni d'un système d'auto-calibration pour le pipeline model :**
   ```
  $ spark-shell -i spark_doctissimo_last_autocalibrated.scala.scala
  ```
