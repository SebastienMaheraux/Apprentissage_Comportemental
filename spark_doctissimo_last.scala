/*
    fullstackml.com
*/


// General purpose library
import scala.xml._
import scala.util.matching.Regex

// Spark data manipulation libraries
import org.apache.spark.sql.catalyst.plans._
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

// Spark machine learning libraries
import org.apache.spark.ml.feature.{HashingTF, Tokenizer}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.ml.Pipeline

val fileName = "data_javaed.xml"

//On charge le fichier texte (qui est dans le même dossier)
val textFile = sc.textFile(fileName)

//Le map va créer une trame de données avec chaque ligne du fichier XML
val postsXml = textFile.map(_.trim).
                    filter(!_.startsWith("<?xml version=")).    //On garde ce qui ne commence PAS par ...
                    filter(!_.startsWith("<forum")).
                    filter(_ != "</forum>")                     //On garde ce qui n'est PAS ...

// On va prendre ce qui nous intéresse dans les lignes ...
val postsRDD = postsXml.map { s =>
            val xml = XML.loadString(s)

            // On va récupérer chaque champ de la balise XML et les mettre dans une variable
            val id = (xml \ "@id").text
			val titleBrut = (xml \ "@title").text
            val messageBrut = (xml \ "@message").text
            val date = (xml \ "@date").text

            // REGULAR EXPRESSION :
                             // \\S : match les !whitespace
                             // .r() method : convertir le text en expression régulière
                             // dans "body", tout ce qui match avec l'expression regulière est remplacé par un espace

			val message = ("<\\S+>".r).replaceAllIn(messageBrut, " ").replaceAll("\n", " ").replaceAll("( )+", " ");

			val title = ("<\\S+>".r).replaceAllIn(titleBrut, " ").replaceAll("\n", " ").replaceAll("( )+", " ");

            // On a une nouvelle variablme qui est la concaténation :
            //      - du titre
            //      - du "bodyPlain" dans lequel les retour à la ligne (\n) et les espaces multiples ( )+ par " "
            val tagsBrut = "<" + title.replaceAll(" ", "><") + ">";

			//val tags = tagsBrut enlevers les balises contentant de mots de moins de 3 caractères
			//val tags = ("<[.{3,}]>".r).replaceAllIn(tagsBrut, "");
			//val tags = ("<[a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9][a-zA-Z0-9]*>".r).replaceAllIn(tagsBrut, "");
			val tags = ("<[a-zA-Z0-9]{1,2}>".r).replaceAllIn(tagsBrut, "");

            // Créé une ligne avec les valeurs ...
            Row(id, title, message, tags, date)
        }


// ******************* Le but est ici de créer une trame de données *********************************
val schemaString = "Id Title Message Tags Date"
val schema = StructType(

      // map method : applique la fonction "StructField" à chaque "fieldName"
      // Ex: val l = List(1,2,3)
      //     l.map( x => x*2 ) donne List(2,4,6)

      // SructField :
      //    fieldName : le champ à traiter (Id, Tags et Text)
      //    StringType : le type du champ
      //    True : car le champ peut être nul
      schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, true)))

// On créé notre fameuse trame de données suivant le schéma spécifié : un tableau avec 3 colonnes (Id, Tags, Text)
// et autant de lignes qu'il y en a dans le XML
val postsDf = sqlContext.createDataFrame(postsRDD, schema)

println("\n XML values")
postsDf.show()

// ****************************************** LABELISER ***********************************

// On va utiliser le tag Alimentation pour prédire
val targetTag = "Alimentation"

// ****** Le classifieur binaire : si une ligne contient le tag "Alimentation", son étiquette sera 1 sinon 0 *****

// muudf (User Defined Function): entrée: ligne (string): si contient "java" => 1.0 en sortie, 0.0 sinon
val myudf: (String => Double) = (str: String) => {if (str.contains(targetTag)) 1.0 else 0.0}
val sqlfunc = udf(myudf)
val postsLabeled = postsDf.withColumn("Label", sqlfunc(col("Tags")) )

val positive = postsLabeled.filter('Label > 0.0)
val negative = postsLabeled.filter('Label < 1.0)

println("\n XML values with LABEL 0.0 if !TAG and 1.0 if TAG")
postsLabeled.show()


// ********************************************************************************************************

// Sample without replacement (false)
// On va séparer en 2 tables pour créer notre TRAINING (90% BDDA) : une avec les lignes ayant le label 1.0 et l'autre 0.0
val positiveTrain = positive.sample(false, 0.9)
val negativeTrain = negative.sample(false, 0.9)
val training = positiveTrain.unionAll(negativeTrain)

println("\n POSITIVE training values")
//positiveTrain.show()
println("\n NEGATIVE training values")
//negativeTrain.show()
println("\n ******** TRAINING : POSITIVE + NEGATIVE training values *********")
training.show()

println(" SUITE ...")

// witColumnRenamed(existingCol,newCol)
val negativeTrainTmp = negativeTrain.withColumnRenamed("Label", "Flag").select('ID, 'Flag)

//negativeTrainTmp.show()

val negativeTest = negative.join( negativeTrainTmp, negative("ID") === negativeTrainTmp("ID"), "LeftOuter").
                            filter("Flag is null").select(negative("ID"), 'Title, 'Message, 'Tags, 'Date, 'Label)

println("\n NEGATIVE testing values")
//negativeTest.show()

val positiveTrainTmp = positiveTrain.withColumnRenamed("Label", "Flag").select('ID, 'Flag)

//positiveTrainTmp.show()

val positiveTest = positive.join(positiveTrainTmp, positive("ID") === positiveTrainTmp("ID"), "LeftOuter").
                            filter("Flag is null").select(positive("ID"), 'Title, 'Message, 'Tags, 'Date, 'Label)

println("\n POSITIVE testing values")
//positiveTest.show()

val testing = negativeTest.unionAll(positiveTest)
val testing2 = positiveTest.unionAll(negativeTest)

println("\n ******** TESTING : POSITVE + NEGATIVE testing values *********")

testing.show()
//testing2.show()

/// **************************** Création du modèle avec 90% des données *************************************

println("OK LET'S GO !")

//val numFeatures = 60
val numFeatures = 64000
val numEpochs = 30
val regParam = 0.02

//Tokeniser, c'est spliter une chaine de caractères en "jetons" (=mots) pour leur appliquer un traitement ensuite
val tokenizer = new Tokenizer().setInputCol("Message").setOutputCol("Words")

// On va s'intéresser à la fréquence d'apparition des mots
//	--> hashingTF : transformation en vecteurs caractéristiques de chaque mot
val hashingTF = new  org.apache.spark.ml.feature.HashingTF().setNumFeatures(numFeatures).
          setInputCol(tokenizer.getOutputCol).setOutputCol("Features")
val lr = new LogisticRegression().setMaxIter(numEpochs).setRegParam(regParam).
                                    setFeaturesCol("Features").setLabelCol("Label").
                                    setRawPredictionCol("Score").setPredictionCol("Prediction")

// le pipeline est une séquence de 3 étapes :
//       tokenizer
//        Hashing TF
//			logistic regression
val pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF, lr))

// fit() est appelée sur le jeux de données d'apprentissage
val model = pipeline.fit(training)

// https://spark.apache.org/docs/1.2.1/ml-guide.html

// *********************************** Test du modèle sur les 10% restants *******************************

val testTitle = "Je cherche à optimiser mon alimentation pour être le plus stock possible en un minimum de temps"
val testMessage = """Tout est dans le titre, j'attends vos idées avec impatience, à moi les bras de schwarZI !!!"""
val testText = testTitle + testMessage

val testDF = sqlContext.createDataFrame(Seq( (99.0, testText))).toDF("Label", "Message")
val result = model.transform(testDF)
val prediction = result.collect()(0)(6).asInstanceOf[Double]
print("Prediction: "+ prediction)

// Itéressons nous à la performance de notre classifieur binaire
val testingResult = model.transform(testing)
val testingResultScores = testingResult.select("Prediction", "Label").rdd.
                                    map(r => (r(0).asInstanceOf[Double], r(1).asInstanceOf[Double]))
val bc = new BinaryClassificationMetrics(testingResultScores)

// Le ROC, c'est une représentation graphique des performances d'un classifieur binaire
//      grâce au taux de vrai positifs et le taux de faux positifs
//          --> plus l'aire sous la courbe ROC est proche de 1 (100%) plus le classifieur binaire est efficace
val roc = bc.areaUnderROC
print("Area under the ROC:" + roc)
