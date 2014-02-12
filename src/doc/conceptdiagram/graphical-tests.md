# Graphical tests

## Create and name concept

**TIP** Do this twice because we'll be deleting two ways later on

1. Instantiate concept

       * do: Drag from template
       * expect: New blank concept

2. Set concept name

      * do: Hover, select text box, type new name
      * expect 1: Labeled concept
      * expect 2: Concept added to class tree (HINT: may need to expand tree)

## Rename the concept

3. Rename visually

      * do: Hover, select text box, replace name
      * expect 1: Relabeled concept
      * expect 2: Concept renamed in class tree
      * relax: Editor seems slow to catch up, may need manual refresh to see new label
      * detail: should be able to rename by hitting enter
      * detail: ... or by mousing out

4. Rename in editor

      * prereq: Activate editor portlet
      * do: Set rdfs:label to new name
      * expect 1: Relabeled concept
      * expect 2: Concept renamed in class tree

5. Repeat rename visually...

      * do/expect: see above
      * PROBLEM 2014-02-12: seems not to work? resets after a while. Could it be slow propagation of the original rename event?

## Delete the concept

5. Delete visually

     * do: Hover on concept, hit X
     * expect 1: Gone from visual editor
     * expect 2: Gone from class tree

6. Delete from class tree

     * do: Select in class tree, delete
     * expect 1: Gone from class tree
     * expect 2: Gone from visual editor

