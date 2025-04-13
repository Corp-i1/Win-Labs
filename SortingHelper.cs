using System;
using System.Collections.Generic;

namespace Win_Labs
{
    internal class SortingHelper
    {
        /*Function: ListMergeSort
         * Description: This function sorts a list using the merge sort algorithm.
         */
        internal List<int> ListMergeSort(List<int> list)
        {
            if (list == null || list.Count <= 1)
                return list;

            // Split the list into two halves
            int middleIndex = list.Count / 2;
            List<int> leftHalf = list.GetRange(0, middleIndex);
            List<int> rightHalf = list.GetRange(middleIndex, list.Count - middleIndex);

            // Recursively sort both halves
            leftHalf = ListMergeSort(leftHalf);
            rightHalf = ListMergeSort(rightHalf);

            // Merge the sorted halves
            List<int> sortedList = new List<int>();
            int leftPointer = 0, rightPointer = 0;

            // Merge elements from both halves in sorted order
            while (leftPointer < leftHalf.Count && rightPointer < rightHalf.Count)
            {
                if (leftHalf[leftPointer] <= rightHalf[rightPointer])
                {
                    sortedList.Add(leftHalf[leftPointer]);
                    leftPointer++;
                }
                else
                {
                    sortedList.Add(rightHalf[rightPointer]);
                    rightPointer++;
                }
            }

            // Add remaining elements from the left half
            while (leftPointer < leftHalf.Count)
            {
                sortedList.Add(leftHalf[leftPointer]);
                leftPointer++;
            }

            // Add remaining elements from the right half
            while (rightPointer < rightHalf.Count)
            {
                sortedList.Add(rightHalf[rightPointer]);
                rightPointer++;
            }

            return sortedList;
        }
        /* Function: SortCues
         * Description: Sorts a list of Cue objects based on a specified property.
         * Parameters:
         *   - cues: The list of Cue objects to sort.
         *   - keySelector: A function to select the key to sort by.
         *   - ascending: Whether to sort in ascending order (default is true).
         * Returns: A sorted list of Cue objects.
         */
        internal List<Cue> SortCues(List<Cue> cues, Func<Cue, object> keySelector, bool ascending = true)
        {
            if (cues == null || keySelector == null)
            {
                throw new ArgumentNullException("Cues or keySelector cannot be null.");
            }

            return ascending
                ? cues.OrderBy(keySelector).ToList()
                : cues.OrderByDescending(keySelector).ToList();
        }
    }


}
