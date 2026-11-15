import './Pagination.css'

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

type PaginationProps = Pick<
  PageResponse<unknown>,
  'totalPages' | 'number' | 'first' | 'last'
> & {
  onPageChange: (newPage: number) => void;
};

export function Pagination({
  totalPages,
  number,
  first,
  last,
  onPageChange,
}: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  // Generate an array [0, 1, 2, ..., totalPages - 1]
  const pageNumbers = Array.from({ length: totalPages }, (_, index) => index);

  return (
    <div className="pages">
      <button
        disabled={first}
        onClick={() => onPageChange(number - 1)}
      >
        Previous
      </button>

      {/* Only show numbered page buttons when there are more than 2 pages */}
      {totalPages > 2 ? (
        pageNumbers.map((pageIndex) => {
          const isActive = pageIndex === number;
          return (
            <button
              key={pageIndex}
              disabled={isActive}
              onClick={() => onPageChange(pageIndex)}
              style={{
                fontWeight: isActive ? 'bold' : 'normal',
                backgroundColor: isActive ? '#333' : undefined,
                color: isActive ? '#fff' : undefined,
              }}
            >
              {pageIndex + 1}
            </button>
          );
        })
      ) : (
        <span>
          Page {number + 1} of {totalPages}
        </span>
      )}

      <button
        disabled={last}
        onClick={() => onPageChange(number + 1)}
      >
        Next
      </button>
    </div>
  );
}
